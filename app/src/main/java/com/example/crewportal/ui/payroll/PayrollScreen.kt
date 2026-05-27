package com.example.crewportal.ui.payroll

import androidx.compose.foundation.clickable
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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
import com.example.crewportal.data.leave.LeaveDatabase
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
                        val activity = context as? FragmentActivity
                        if (activity == null) {
                            Toast.makeText(
                                context,
                                if (ru) "Биометрия недоступна. Используйте пароль." else "Biometric authentication is unavailable. Use password.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OutlinedButton
                        }

                        val biometricManager = BiometricManager.from(context)
                        val canAuthenticate = biometricManager.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG
                        )

                        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                            Toast.makeText(
                                context,
                                if (ru) "Биометрия не настроена. Используйте пароль." else "Biometrics are not set up. Use password.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OutlinedButton
                        }

                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle(if (ru) "Доступ к зарплате" else "Payroll access")
                            .setSubtitle(if (ru) "Подтвердите личность" else "Confirm your identity")
                            .setNegativeButtonText(if (ru) "Отмена" else "Cancel")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .build()

                        val prompt = BiometricPrompt(
                            activity,
                            ContextCompat.getMainExecutor(context),
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    unlocked = true
                                    Toast.makeText(
                                        context,
                                        if (ru) "Доступ разрешён" else "Payroll unlocked",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                                        Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        prompt.authenticate(promptInfo)
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (ru) "Использовать отпечаток" else "Use fingerprint") }
                }
            }
        } else {
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
    val today = LocalDate.now()
    val availableMonths = generateSequence(YearMonth.of(2026, 5)) { it.plusMonths(1) }
        .takeWhile { it.isBefore(YearMonth.from(today)) || (it == YearMonth.from(today) && today.dayOfMonth >= 5) }
        .filter { month -> today >= month.plusMonths(1).atDay(5) }
        .toList()
        .sortedDescending()
    var selectedMonth by remember { mutableStateOf<YearMonth?>(null) }

    if (selectedMonth == null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (ru) "Расчётные листы" else "Monthly payslips", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (availableMonths.isEmpty()) {
                    Text(
                        if (ru) "May 2026 salary has not been calculated yet. Payslip will be available on 05 Jun 2026."
                        else "May 2026 salary has not been calculated yet. Payslip will be available on 05 Jun 2026.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    availableMonths.forEach { month ->
                        val calc = calculatePayslip(month, flights)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().clickable { selectedMonth = month }
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year} Salary", fontWeight = FontWeight.Bold)
                                Text("Ready • Net Pay: $${calc.net} • Block: ${formatMinutes(calc.blockMinutes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    } else {
        val month = selectedMonth!!
        val calc = calculatePayslip(month, flights)
        OutlinedButton(onClick = { selectedMonth = null }, modifier = Modifier.fillMaxWidth()) {
            Text(if (ru) "Назад к списку" else "Back to payslips")
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Payslip — ${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Thai Airways International", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Employee: Dmitrii Alekseev • ID 141901 • Captain", fontWeight = FontWeight.SemiBold)
                SectionTitle(if (ru) "Основа расчёта" else "Payroll basis")
                Text("Block time: ${formatMinutes(calc.blockMinutes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Briefing / debriefing: ${formatMinutes(calc.dutyMinutes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Reserve days: ${calc.reserveDays}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Leave days: ${calc.leaveDays}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                SectionTitle(if (ru) "Начисления" else "Earnings")
                AmountRow("Flight Pay", calc.flightPay)
                AmountRow("Briefing / Debriefing Pay", calc.dutyPay)
                if (calc.reservePay > 0) AmountRow("Reserve / Standby Pay", calc.reservePay)
                if (calc.deadheadPay > 0) AmountRow("Deadhead Pay", calc.deadheadPay)
                if (calc.nightPremium > 0) AmountRow("Night Premium", calc.nightPremium)
                if (calc.holidayPremium > 0) AmountRow("Holiday Premium", calc.holidayPremium)
                if (calc.augmentedRestPay > 0) AmountRow("Augmented Crew Rest Pay", calc.augmentedRestPay)
                if (calc.layoverPay > 0) AmountRow("Meal / Layover", calc.layoverPay)
                if (calc.leavePay > 0) AmountRow("Leave Pay", calc.leavePay)
                if (calc.unusedLeaveCompensation > 0) AmountRow("Unused Leave Compensation", calc.unusedLeaveCompensation)
                AmountRow("Total Earnings", calc.gross, bold = true)
                Spacer(Modifier.height(6.dp))
                SectionTitle(if (ru) "Удержания" else "Deductions")
                AmountRow("Income Tax", -calc.tax)
                AmountRow("Provident Fund", -calc.provident)
                AmountRow("Health Insurance", -calc.insurance)
                AmountRow("Crew Welfare", -calc.welfare)
                AmountRow("Total Deductions", -calc.deductions, bold = true)
                Spacer(Modifier.height(6.dp))
                AmountRow("Net Pay", calc.net, bold = true)
                Text("${calc.net.toWords()} Dollars", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

data class PayslipCalc(
    val blockMinutes: Int,
    val dutyMinutes: Int,
    val reserveDays: Int,
    val leaveDays: Int,
    val flightPay: Int,
    val dutyPay: Int,
    val reservePay: Int,
    val deadheadPay: Int,
    val nightPremium: Int,
    val holidayPremium: Int,
    val augmentedRestPay: Int,
    val layoverPay: Int,
    val leavePay: Int,
    val unusedLeaveCompensation: Int,
    val gross: Int,
    val tax: Int,
    val provident: Int,
    val insurance: Int,
    val welfare: Int,
    val deductions: Int,
    val net: Int
)

private fun calculatePayslip(month: YearMonth, flights: List<FlightEntity>): PayslipCalc {
    val prefix = "%04d-%02d".format(month.year, month.monthValue)
    val monthDuties = flights.filter { it.departureDateTime.startsWith(prefix) }
    val completedFlights = monthDuties.filter { it.dutyType == "FLIGHT" && it.isCompleted }
    val sourceFlights = completedFlights.ifEmpty { monthDuties.filter { it.dutyType == "FLIGHT" } }
    val blockMinutes = sourceFlights.sumOf { it.durationMinutes }
    val dutyMinutes = sourceFlights.count() * 90 + sourceFlights.groupBy { it.departureDateTime.take(10) }.size * 30
    val reserveDays = monthDuties.count { it.dutyType == "RESERVE" }
    val leaveDays = LeaveDatabase.leaveDaysInMonth(month)
    val longHaulMinutes = sourceFlights.filter { it.durationMinutes >= 540 }.sumOf { it.durationMinutes }
    val layoverDays = monthDuties.count { it.dutyType == "STAY" }

    val flightPay = ((blockMinutes / 60.0) * 95).roundToInt()
    val dutyPay = ((dutyMinutes / 60.0) * 24).roundToInt()
    val reservePay = reserveDays * 120
    val deadheadPay = monthDuties.count { it.dutyType == "DEADHEAD" } * 180
    val nightPremium = ((sourceFlights.sumOf { nightMinutes(it) } / 60.0) * 95).roundToInt()
    val holidayPremium = sourceFlights.filter { isHoliday(it.departureDateTime.take(10)) }.sumOf { ((it.durationMinutes / 60.0) * 95 * 1.5).roundToInt() }
    val augmentedRestPay = ((longHaulMinutes / 2.0 / 60.0) * 95 * 0.75).roundToInt()
    val layoverPay = layoverDays * 95
    val dailyLeavePay = 180
    val leavePay = leaveDays * dailyLeavePay
    val unusedLeaveCompensation = if (month.monthValue == 1) {
        val used = 40 - 0 // placeholder until prior-year leave balance exists in persistent state
        maxOf(0, 40 - used) * dailyLeavePay * 2
    } else 0
    val gross = flightPay + dutyPay + reservePay + deadheadPay + nightPremium + holidayPremium + augmentedRestPay + layoverPay + leavePay + unusedLeaveCompensation
    val tax = (gross * 0.12).roundToInt()
    val provident = (gross * 0.04).roundToInt()
    val insurance = if (gross > 0) 90 else 0
    val welfare = if (gross > 0) 35 else 0
    val deductions = tax + provident + insurance + welfare
    val net = gross - deductions
    return PayslipCalc(blockMinutes, dutyMinutes, reserveDays, leaveDays, flightPay, dutyPay, reservePay, deadheadPay, nightPremium, holidayPremium, augmentedRestPay, layoverPay, leavePay, unusedLeaveCompensation, gross, tax, provident, insurance, welfare, deductions, net)
}

private fun nightMinutes(flight: FlightEntity): Int {
    val start = java.time.LocalDateTime.parse(flight.departureDateTime)
    val end = java.time.LocalDateTime.parse(flight.arrivalDateTime)
    var cursor = start
    var minutes = 0
    while (cursor.isBefore(end)) {
        val hour = cursor.hour
        if (hour >= 23 || hour < 6) minutes++
        cursor = cursor.plusMinutes(1)
    }
    return minutes
}

private fun isHoliday(date: String): Boolean = date.endsWith("01-01") || date.endsWith("04-13") || date.endsWith("04-14") || date.endsWith("12-05") || date.endsWith("12-10")

@Composable
private fun BonusTab(flights: List<FlightEntity>, ru: Boolean) {
    val today = LocalDate.now()
    val availableMonths = generateSequence(YearMonth.of(2026, 5)) { it.plusMonths(1) }
        .takeWhile { it.isBefore(YearMonth.from(today)) || (it == YearMonth.from(today) && today.dayOfMonth >= 5) }
        .filter { month -> today >= month.plusMonths(1).atDay(5) }
        .toList()
        .sortedDescending()

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (ru) "Премия" else "Bonus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (availableMonths.isEmpty()) {
                Text(
                    if (ru) "Премия за месяц ещё не рассчитана. Она появится вместе с расчётным листком."
                    else "Monthly bonus has not been calculated yet. It will be available together with the payslip.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val month = availableMonths.first()
                val amount = 950 + ((flights.count { it.departureDateTime.startsWith("%04d-%02d".format(month.year, month.monthValue)) } * 37) % 420)
                Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year} • 100%", fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(progress = 1f, modifier = Modifier.fillMaxWidth())
                AmountRow(if (ru) "Сумма премии" else "Bonus Amount", amount, bold = true)
                Text(if (ru) "Safety, performance and training compliance: 100%" else "Safety, performance and training compliance: 100%", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
@Composable private fun AmountRow(label: String, amount: Int, bold: Boolean = false) { Row(Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal); Text("${if (amount < 0) "-" else ""}\$${kotlin.math.abs(amount)}", fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal) } }
private fun Int.toWords(): String = when (this) { in 0..9999 -> "${this}" else -> this.toString() }
