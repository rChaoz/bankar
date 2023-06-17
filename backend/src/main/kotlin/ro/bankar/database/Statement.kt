package ro.bankar.database

import com.lowagie.text.Cell
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.Table
import com.lowagie.text.alignment.HorizontalAlignment
import com.lowagie.text.alignment.VerticalAlignment
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfWriter
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SStatement
import ro.bankar.plugins.init
import ro.bankar.util.atEndOfDayIn
import ro.bankar.util.format
import ro.bankar.util.formatIBAN
import ro.bankar.util.nowHere
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.DecimalFormat

class Statement(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Statement>(Statements) {
        fun generate(name: String?, account: BankAccount, dateRange: ClosedRange<LocalDate>, timeZone: TimeZone): Statement {
            // Calculate period in user's timezone
            val start = dateRange.start.atStartOfDayIn(timeZone)
            val end = dateRange.endInclusive.atEndOfDayIn(timeZone)
            val range = start..end
            // Generate statement
            val transfers = BankTransfer.findInPeriod(account, range)
            val transactions = CardTransaction.findInPeriod(account, range)
            val (stream, document) = createPDF()
            document.generateStatement(name, dateRange, timeZone, account, transfers, transactions)
            // Insert into database
            return new {
                this.name = name
                bankAccount = account
                statement = ExposedBlob(stream.toByteArray())
            }
        }

        fun findByUser(user: User) = find { Statements.bankAccount inList user.bankAccountIds }
    }

    fun serializable() = SStatement(id.value, name, timestamp, bankAccount.id.value)

    var name by Statements.name
    var bankAccount by BankAccount referencedOn Statements.bankAccount
    var timestamp by Statements.timestamp
    var statement by Statements.statement
}

fun SizedIterable<Statement>.serializable() = map(Statement::serializable)

object Statements : IntIdTable() {
    val name = varchar("name", 20).nullable()
    val bankAccount = reference("bank_account", BankAccounts, onDelete = ReferenceOption.CASCADE)
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp())
    val statement = blob("statement")
}

// For testing
fun main() {
    Database.init()
    loadStaticData()

    val dateRange = LocalDate(2023, Month.APRIL, 25)..LocalDate(2023, Month.MAY, 30)
    val timeZone = TimeZone.currentSystemDefault()
    val range = dateRange.start.atStartOfDayIn(timeZone)..dateRange.endInclusive.atEndOfDayIn(timeZone)
    val (stream, doc) = createPDF()

    transaction {
        val account = BankAccount.findById(1)!!
        doc.generateStatement("My Statement", dateRange, timeZone,
            account, BankTransfer.findInPeriod(account, range), CardTransaction.findInPeriod(account, range))
    }

    stream.writeTo(FileOutputStream("test.pdf"))
}

// PDF Generator
private fun createPDF(): Pair<ByteArrayOutputStream, Document> {
    val o = object {} // used to access resources

    // Create the PDF stream & writer
    val stream = ByteArrayOutputStream()
    val document = Document(PageSize.A4, 36f, 36f, 45f, 50f)
    val writer = PdfWriter.getInstance(document, stream)
    writer.pageEvent = HeaderAndFooterHelper

    // Write metadata
    document.addCreationDate()
    document.addSubject("BanKAR Account Statement")
    document.addCreator("BanKAR")
    document.addAuthor("BanKAR")

    // Open document for writing
    document.open()

    // Add the initial content (logo & information)
    val table = Table(2).apply {
        width = 100f
        border = Rectangle.NO_BORDER
        defaultCell.border = Rectangle.NO_BORDER
    }
    table.addCell(
        Cell(Image.getInstance(o.javaClass.getResource("/statement/logo.png")).apply { scalePercent(10f) }).apply {
            setHorizontalAlignment(HorizontalAlignment.CENTER)
            setVerticalAlignment(VerticalAlignment.CENTER)
        }
    )
    table.addCell(
        Paragraph(
            """
        BanKAR S.A.
        
        BanKAR is a modern online banking system that simplifies and improves the online banking experience.
        SWIFT: RBNK RO BU
        Website: www.bankar.ro
        E-mail: contact@bankar.ro
        Phone: +40 726 275 224
        BanKAR is not actually real. This is a college project created to explore the Android & Compose systems in the context of online banking.
    """.trimIndent(), Font(Font.HELVETICA, 9f)
        )
    )
    table.addCell(Cell(Paragraph(Clock.System.nowHere().format(true), Font(Font.HELVETICA, 9f))).apply {
        colspan = 2
        setHorizontalAlignment(HorizontalAlignment.RIGHT)
    })
    document.add(table)

    return stream to document
}

/**
 * Must be called inside an SQL transaction
 */
private fun Document.generateStatement(
    name: String?,
    range: ClosedRange<LocalDate>,
    timeZone: TimeZone,
    account: BankAccount,
    transfers: Iterable<BankTransfer>,
    transactions: Iterable<CardTransaction>
) {
    // Add title
    add(
        Paragraph(
            "Statement${if (name == null) "" else " \"$name\""} of ${Clock.System.todayIn(timeZone).format()}",
            Font(Font.HELVETICA, 16f, Font.BOLD)
        ).apply { spacingBefore = 15f; alignment = Element.ALIGN_CENTER }
    )
    add(Paragraph("period: ${range.start.format()} - ${range.endInclusive.format()}", Font(Font.HELVETICA, 14f, Font.BOLDITALIC)).apply {
        alignment = Element.ALIGN_CENTER
    })

    // Fonts used in PDF
    val bold = Font(Font.HELVETICA, 10f, Font.BOLD)
    val italic = Font(Font.HELVETICA, 10f, Font.ITALIC)
    val normal = Font(Font.HELVETICA, 10f)
    val subtitle = Font(Font.HELVETICA, 12f, Font.BOLD)

    val decimalFormat = DecimalFormat("#.##")
    // Add account & owner details
    add(Table(3).apply {
        width = 100f
        setWidths(floatArrayOf(2f, 5f, 3f))
        border = Rectangle.NO_BORDER
        defaultCell.border = Rectangle.NO_BORDER

        val owner = account.user
        addCell(Cell(Phrase("Account owner:", bold)).apply {
            rowspan = 5
            add(Phrase("\n${owner.fullName}\n", normal))
            add(Phrase("${COUNTRY_DATA.find { it.code == owner.countryCode }?.country ?: owner.countryCode}, ${owner.state}\n", normal))
            add(Phrase(owner.city, normal))
        }, 0, 2)

        addCell(Phrase("Account type:", bold))
        addCell(Phrase(account.type.title, normal))

        addCell(Phrase("Account IBAN:", bold))
        addCell(Phrase(formatIBAN(account.iban), normal))

        addCell(Phrase("Currency:", bold))
        addCell(Phrase(account.currency.code, normal))

        addCell(Phrase("Balance:", bold))
        addCell(Phrase(decimalFormat.format(account.balance.toDouble()), normal))

        if (account.type == SBankAccountType.Credit) {
            addCell(Phrase("Credit limit:", bold))
            addCell(Phrase(decimalFormat.format(account.limit.toDouble()), normal))
        }
    })

    // Start with a date in the future to ensure date header is added for the first transfer/transaction
    var currentDate = Clock.System.todayIn(TimeZone.UTC) + DatePeriod(days = 5)

    // Current table used to add data
    val startTable = Table(1)
    var table = startTable
    var balance = 0.0

    fun endTable() {
        if (table === startTable) return
        table.addCell(Cell().apply { colspan = 2; border = Rectangle.NO_BORDER })
        table.addCell(Cell(Phrase("Total amount:", bold)).apply { setHorizontalAlignment(HorizontalAlignment.RIGHT); border = Rectangle.NO_BORDER })
        table.addCell(Cell(Phrase(decimalFormat.format(balance), normal))
            .apply { setHorizontalAlignment(HorizontalAlignment.RIGHT); border = Rectangle.NO_BORDER })
        balance = 0.0
        add(table)
    }

    fun beginTable(date: LocalDate) = Table(4).apply {
        // End existing table (if there is one)
        endTable()

        currentDate = date
        width = 100f
        padding = 2f
        setWidths(floatArrayOf(2f, 4.5f, 2.5f, 1f))
        border = Rectangle.NO_BORDER

        addCell(Cell(Phrase("Date:", subtitle)).apply { border = Rectangle.NO_BORDER; rowspan = 2; setVerticalAlignment(VerticalAlignment.CENTER) })
        addCell(Cell(Phrase(date.format(), subtitle)).apply { border = Rectangle.NO_BORDER; rowspan = 2; colspan = 3 })

        // Table header
        addCell(Cell(Phrase("Datetime", bold)).apply { border = Rectangle.NO_BORDER })
        addCell(Cell(Phrase("Details", bold)).apply { border = Rectangle.NO_BORDER })
        addCell(Cell(Phrase("Exchange info", bold)).apply { border = Rectangle.NO_BORDER })
        addCell(Cell(Phrase("Amount", bold)).apply { border = Rectangle.NO_BORDER })
    }

    data class ExchangeInfo(
        val onArrival: Boolean,
        val from: Currency,
        val to: Currency,
        val fromAmount: Double,
        val toAmount: Double
    )

    fun Table.addLine(dateTime: LocalDateTime, details: String, exchangeInfo: ExchangeInfo?, amount: Double) {
        balance += amount
        addCell(Cell(Phrase(dateTime.format(true), normal)))
        addCell(Cell(Phrase(details, normal)))
        if (exchangeInfo == null) addCell(Cell(Phrase("No exchange occurred", italic)))
        else addCell(Cell(Phrase(with(exchangeInfo) {
            """
            ${if (onArrival) "Exchanged occurred on arrival" else "Exchanged occurred"}
            From $fromAmount ${from.code} to  $toAmount ${to.code}
            Rate: ${decimalFormat.format(1.0)} ${from.code} = ${decimalFormat.format(toAmount / fromAmount)} ${to.code}
            """.trimIndent()
        }, normal)))
        addCell(Cell(Phrase(decimalFormat.format(amount), normal)).apply { setHorizontalAlignment(HorizontalAlignment.RIGHT) })
    }

    // Go through transfers & transactions
    val transferI = transfers.iterator()
    val transactionI = transactions.iterator()
    var transfer: BankTransfer? = if (transferI.hasNext()) transferI.next() else null
    var transaction: CardTransaction? = if (transactionI.hasNext()) transactionI.next() else null
    while (transfer != null || transaction != null) {
        if (transfer != null && (transaction == null || transfer.timestamp > transaction.timestamp)) {
            val dateTime = transfer.timestamp.toLocalDateTime(timeZone)
            if (dateTime.date != currentDate) table = beginTable(dateTime.date)
            val sent = transfer.sender == account
            table.addLine(dateTime, "${
                if (sent) "To: ${transfer.recipientName}\n${transfer.recipientIban}"
                else "From: ${transfer.senderName}\n${transfer.senderIban}"
            }\nTransfer note: ${transfer.note}",
                if (sent || transfer.exchangedAmount == null) null else ExchangeInfo(
                    false, transfer.currency, account.currency, transfer.amount.toDouble(), transfer.exchangedAmount!!.toDouble()
                ), (if (sent) transfer.amount else (transfer.exchangedAmount ?: transfer.amount)).let { if (sent) -it.toDouble() else it.toDouble() })
            transfer = if (transferI.hasNext()) transferI.next() else null
        } else {
            val dateTime = transaction!!.timestamp.toLocalDateTime(timeZone)
            if (dateTime.date != currentDate) table = beginTable(dateTime.date)
            // TODO Implement exchanging for transactions
            table.addLine(dateTime, transaction.details, null, transaction.amount.toDouble())
            transaction = if (transactionI.hasNext()) transactionI.next() else null
        }
    }

    // Finish table & document
    endTable()
    close()
}

private object HeaderAndFooterHelper : PdfPageEventHelper() {
    override fun onStartPage(writer: PdfWriter, document: Document) {
        val table = PdfPTable(1).apply { totalWidth = 530f }

        table.addCell(PdfPCell(Paragraph("BanKAR • Account statement", Font(Font.COURIER, 15f, Font.BOLD))).apply {
            horizontalAlignment = Element.ALIGN_CENTER
            paddingBottom = 5f
            border = Rectangle.BOTTOM
        })

        table.writeSelectedRows(0, -1, 34f, 828f, writer.directContent)
    }

    override fun onEndPage(writer: PdfWriter, document: Document) {
        val table = PdfPTable(2).apply { totalWidth = 530f }

        table.addCell(PdfPCell(Paragraph("BanKAR", Font(Font.COURIER, 11f))).apply {
            paddingTop = 4f
            horizontalAlignment = Element.ALIGN_LEFT
            border = Rectangle.TOP
        })

        table.addCell(PdfPCell(Paragraph("page ${document.pageNumber}", Font(Font.COURIER, 11f))).apply {
            paddingTop = 4f
            horizontalAlignment = Element.ALIGN_RIGHT
            border = Rectangle.TOP
        })

        table.writeSelectedRows(0, -1, 34f, 36f, writer.directContent)
    }
}