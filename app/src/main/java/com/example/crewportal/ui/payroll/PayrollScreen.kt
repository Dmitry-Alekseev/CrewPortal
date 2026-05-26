package com.example.crewportal.ui.payroll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.util.formatMinutes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PayrollScreen(
    flightRepository: FlightRepository,
    preferencesRepository: PreferencesRepository,
    ru: Boolean
) {
    val flights by flightRepository.observeFlights().collectAsState(initial = emptyList())
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(if (ru) "Зарплата" else "Payroll", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        if (!unlocked) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (ru) "Защищённый раздел" else "Protected payroll area", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (ru) "Введите пароль личного кабинета или используйте биометрию." else "Enter profile password or use biometric access.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = false },
                        label = { Text(if (ru) "Пароль" else "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = error,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error) Text(if (ru) "Неверный пароль" else "Incorrect password", color = MaterialTheme.colorScheme.error)
                    Button(onClick = {
                        if (password == "Airbus1998") unlocked = true else error = true
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Открыть расчётный лист" else "Unlock payroll") }
                    OutlinedButton(onClick = {
                        Toast.makeText(context, if (ru) "Биометрия будет запрошена системой. Пока используйте пароль." else "Biometric prompt is not available in this build. Use password.", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Использовать отпечаток" else "Use fingerprint") }
                }
            }
        } else {
            SecurityNote(ru)
            SalaryTab(flights = flights, ru = ru)
            BonusTab(flights = flights, ru = ru)
        }
    }
}

@Composable
private fun SecurityNote(ru: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (ru) "Защищённый раздел" else "Protected payroll area", fontWeight = FontWeight.Bold)
            Text(if (ru) "Доступ к расчётному листу защищается паролем или биометрией при входе в личный кабинет." else "Payslip access follows profile password / biometric protection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SalaryTab(flights: List<FlightEntity>, ru: Boolean) {
    val month = YearMonth.of(2026, 5)
    val monthFlights = flights.filter { it.dutyType == "FLIGHT" && it.departureDateTime.startsWith("2026-05") }
    val completed = monthFlights.filter { it.isCompleted }
    val source = if (completed.isNotEmpty()) completed else monthFlights
    val blockMinutes = source.sumOf { it.durationMinutes }
    val blockHours = blockMinutes / 60.0
    val internationalSectors = source.count { it.departureIata != "BKK" || it.arrivalIata !in setOf("HKT", "CNX", "KBV") }
    val reserveDays = flights.count { it.dutyType == "RESERVE" && it.departureDateTime.startsWith("2026-05") }
    val longHaul = source.filter { it.durationMinutes >= 540 }
    val briefingMinutes = source.count() * 90 + source.groupBy { it.departureDateTime.take(10) }.size * 30
    val layoverDays = longHaul.size.coerceAtLeast(0)

    val basic = 7800
    val flightPay = (blockHours * 95).roundToInt()
    val briefingPay = ((briefingMinutes / 60.0) * 22).roundToInt()
    val intlBonus = internationalSectors * 75
    val layover = layoverDays * 140
    val reserve = reserveDays * 110
    val safety = 650
    val gross = basic + flightPay + briefingPay + intlBonus + layover + reserve + safety
    val tax = (gross * 0.18).roundToInt()
    val provident = (gross * 0.05).roundToInt()
    val insurance = 180
    val welfare = 75
    val deductions = tax + provident + insurance + welfare
    val net = gross - deductions

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (ru) "Расчётный лист — May 2026" else "Payslip — May 2026", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Thai Airways International", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Employee: Dmitrii Alekseev • ID 141901 • Captain", fontWeight = FontWeight.SemiBold)
            Text("Working block hours: ${formatMinutes(blockMinutes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            SectionTitle(if (ru) "Начисления" else "Earnings")
            AmountRow("Basic Pay", basic)
            AmountRow("Flight Pay", flightPay)
            AmountRow("Briefing / Debriefing Pay", briefingPay)
            if (intlBonus > 0) AmountRow("International Sector Bonus", intlBonus)
            if (layover > 0) AmountRow("Meal / Layover", layover)
            if (reserve > 0) AmountRow("Reserve / Standby Pay", reserve)
            AmountRow("Safety & Performance", safety)
            AmountRow("Total Earnings", gross, bold = true)
            Spacer(Modifier.height(6.dp))
            SectionTitle(if (ru) "Удержания" else "Deductions")
            AmountRow("Income Tax", -tax)
            AmountRow("Provident Fund", -provident)
            AmountRow("Health Insurance", -insurance)
            AmountRow("Crew Welfare", -welfare)
            AmountRow("Total Deductions", -deductions, bold = true)
            Spacer(Modifier.height(6.dp))
            AmountRow("Net Pay", net, bold = true)
            Text("${net.toWords()} Dollars", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BonusTab(flights: List<FlightEntity>, ru: Boolean) {
    val amount = 950 + ((flights.size * 37) % 420)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (ru) "Премия" else "Bonus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (ru) "Премия за месяц: 100%" else "Monthly bonus: 100%", fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(progress = 1f, modifier = Modifier.fillMaxWidth())
            AmountRow(if (ru) "Сумма премии" else "Bonus Amount", amount, bold = true)
            Text(if (ru) "Safety, performance and training compliance: 100%" else "Safety, performance and training compliance: 100%", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
@Composable private fun AmountRow(label: String, amount: Int, bold: Boolean = false) { Row(Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal); Text("${if (amount < 0) "-" else ""}\$${kotlin.math.abs(amount)}", fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal) } }
private fun Int.toWords(): String = when (this) { in 0..9999 -> "${this}" else -> this.toString() }
