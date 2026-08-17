package com.example.crewportal.data.payroll

import com.example.crewportal.data.leave.LeaveDatabase
import com.example.crewportal.data.local.DutyType
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.roster.RosterMetrics
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.roundToInt

data class PayslipCalc(
    val blockMinutes: Int, val dutyMinutes: Int, val reserveDays: Int, val leaveDays: Int,
    val flightPay: Int, val dutyPay: Int, val reservePay: Int, val deadheadPay: Int,
    val nightPremium: Int, val holidayPremium: Int, val augmentedRestPay: Int,
    val layoverPay: Int, val leavePay: Int, val unusedLeaveCompensation: Int,
    val gross: Int, val tax: Int, val provident: Int, val insurance: Int,
    val welfare: Int, val deductions: Int, val net: Int
)

/** Pure payroll policy; UI only renders this result. */
object PayrollCalculator {
    fun calculate(month: YearMonth, flights: List<FlightEntity>): PayslipCalc {
        val monthDuties = RosterMetrics.dutiesForMonth(flights, month)
        val completed = monthDuties.filter { it.dutyType == DutyType.FLIGHT.value && it.isCompleted }
        val sourceFlights = completed.ifEmpty { monthDuties.filter { it.dutyType == DutyType.FLIGHT.value } }
        val blockMinutes = sourceFlights.sumOf { it.durationMinutes }
        val dutyMinutes = sourceFlights.count() * 90 + sourceFlights.groupBy { it.departureDateTime.take(10) }.size * 30
        val reserveDays = monthDuties.count { it.dutyType == DutyType.RESERVE.value }
        val leaveDays = LeaveDatabase.leaveDaysInMonth(month)
        val longHaulMinutes = sourceFlights.filter { it.durationMinutes >= 540 }.sumOf { it.durationMinutes }
        val layoverDays = monthDuties.count { it.dutyType == DutyType.STAY.value }
        val groundTypes = setOf(DutyType.SIMULATOR.value, DutyType.MEDICAL.value, DutyType.SAFETY.value)
        val paidGroundMinutes = monthDuties.filter { it.dutyType in groundTypes }.sumOf {
            if (it.durationMinutes > 0) it.durationMinutes
            else java.time.Duration.between(LocalDateTime.parse(it.departureDateTime), LocalDateTime.parse(it.arrivalDateTime)).toMinutes().toInt()
        }
        val flightPay = ((blockMinutes / 60.0) * 95).roundToInt()
        val dutyPay = (((dutyMinutes + paidGroundMinutes) / 60.0) * 24).roundToInt()
        val reservePay = reserveDays * 120
        val deadheadPay = monthDuties.count { it.dutyType == DutyType.DEADHEAD.value } * 180
        val nightPremium = ((sourceFlights.sumOf(::nightMinutes) / 60.0) * 95).roundToInt()
        val holidayPremium = sourceFlights.filter { isHoliday(it.departureDateTime.take(10)) }
            .sumOf { ((it.durationMinutes / 60.0) * 95 * 1.5).roundToInt() }
        val augmentedRestPay = ((longHaulMinutes / 2.0 / 60.0) * 95 * 0.75).roundToInt()
        val layoverPay = layoverDays * 95
        val dailyLeavePay = 180
        val leavePay = leaveDays * dailyLeavePay
        val unusedLeaveCompensation = 0 // Requires a closed prior-year balance; never fabricate it.
        val gross = flightPay + dutyPay + reservePay + deadheadPay + nightPremium + holidayPremium + augmentedRestPay + layoverPay + leavePay
        val tax = (gross * 0.12).roundToInt()
        val provident = (gross * 0.04).roundToInt()
        val insurance = if (gross > 0) 90 else 0
        val welfare = if (gross > 0) 35 else 0
        val deductions = tax + provident + insurance + welfare
        return PayslipCalc(blockMinutes, dutyMinutes, reserveDays, leaveDays, flightPay, dutyPay, reservePay, deadheadPay, nightPremium, holidayPremium, augmentedRestPay, layoverPay, leavePay, unusedLeaveCompensation, gross, tax, provident, insurance, welfare, deductions, gross - deductions)
    }

    private fun nightMinutes(flight: FlightEntity): Int {
        var cursor = LocalDateTime.parse(flight.departureDateTime)
        val end = LocalDateTime.parse(flight.arrivalDateTime)
        var minutes = 0
        while (cursor.isBefore(end)) {
            if (cursor.hour >= 23 || cursor.hour < 6) minutes++
            cursor = cursor.plusMinutes(1)
        }
        return minutes
    }

    private fun isHoliday(date: String): Boolean =
        date.endsWith("01-01") || date.endsWith("04-13") || date.endsWith("04-14") ||
            date.endsWith("12-05") || date.endsWith("12-10")
}
