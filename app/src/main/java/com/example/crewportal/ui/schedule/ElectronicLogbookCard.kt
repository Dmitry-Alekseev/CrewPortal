package com.example.crewportal.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.crewportal.data.local.FlightEntity
import com.example.crewportal.data.repository.LogbookRepository
import kotlinx.coroutines.launch

/** Fillable electronic logbook embedded directly in the flight details tab. */
@Composable
fun ElectronicLogbookCard(flight: FlightEntity, repository: LogbookRepository) {
    val stored by repository.observeForFlight(flight.id).collectAsState(initial = null)
    var draft by remember(flight.id, stored?.updatedAtEpochMillis) {
        mutableStateOf(stored ?: repository.prefilled(flight))
    }
    val scope = rememberCoroutineScope()
    val certified = draft.certifiedAtEpochMillis != null
    var message by remember(flight.id) { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Electronic Pilot Logbook",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (certified) "Certified record • locked" else "EASA FCL.050 / AMC1 compatible flight record",
                    color = if (certified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogTextField("Date", draft.date, certified, Modifier.weight(1f)) { draft = draft.copy(date = it) }
                LogTextField("Flight", draft.flightNumber, certified, Modifier.weight(1f)) { draft = draft.copy(flightNumber = it.uppercase()) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogTextField("From", draft.departurePlace, certified, Modifier.weight(1f)) { draft = draft.copy(departurePlace = it.uppercase()) }
                LogTextField("Off blocks", draft.departureTime, certified, Modifier.weight(1f)) { draft = draft.copy(departureTime = it) }
                LogTextField("To", draft.arrivalPlace, certified, Modifier.weight(1f)) { draft = draft.copy(arrivalPlace = it.uppercase()) }
                LogTextField("On blocks", draft.arrivalTime, certified, Modifier.weight(1f)) { draft = draft.copy(arrivalTime = it) }
            }
            LogTextField("Aircraft make / model / variant", draft.aircraftType, certified) { draft = draft.copy(aircraftType = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogTextField("Registration", draft.registration, certified, Modifier.weight(1f)) { draft = draft.copy(registration = it.uppercase()) }
                LogTextField("Pilot function", draft.pilotFunction, certified, Modifier.weight(1f)) { draft = draft.copy(pilotFunction = it.uppercase()) }
            }
            LogTextField("PIC name", draft.picName, certified) { draft = draft.copy(picName = it) }

            Text("Flight time (minutes)", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogNumberField("Total", draft.totalTimeMinutes, certified, Modifier.weight(1f)) { draft = draft.copy(totalTimeMinutes = it) }
                LogNumberField("PIC", draft.picMinutes, certified, Modifier.weight(1f)) { draft = draft.copy(picMinutes = it) }
                LogNumberField("SIC", draft.sicMinutes, certified, Modifier.weight(1f)) { draft = draft.copy(sicMinutes = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogNumberField("Night", draft.nightMinutes, certified, Modifier.weight(1f)) { draft = draft.copy(nightMinutes = it) }
                LogNumberField("IFR", draft.ifrMinutes, certified, Modifier.weight(1f)) { draft = draft.copy(ifrMinutes = it) }
                LogNumberField("Instrument", draft.instrumentMinutes, certified, Modifier.weight(1f)) { draft = draft.copy(instrumentMinutes = it) }
            }

            Text("Take-offs / landings / approaches", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogNumberField("T/O day", draft.takeoffsDay, certified, Modifier.weight(1f)) { draft = draft.copy(takeoffsDay = it) }
                LogNumberField("T/O night", draft.takeoffsNight, certified, Modifier.weight(1f)) { draft = draft.copy(takeoffsNight = it) }
                LogNumberField("LDG day", draft.landingsDay, certified, Modifier.weight(1f)) { draft = draft.copy(landingsDay = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogNumberField("LDG night", draft.landingsNight, certified, Modifier.weight(1f)) { draft = draft.copy(landingsNight = it) }
                LogNumberField("Approaches", draft.approaches, certified, Modifier.weight(1f)) { draft = draft.copy(approaches = it) }
            }

            LogTextField("Remarks / operational notes", draft.remarks, certified) { draft = draft.copy(remarks = it) }
            LogTextField("Electronic signature (full name)", draft.signatureName, certified) { draft = draft.copy(signatureName = it) }

            if (!certified) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                runCatching { repository.saveDraft(draft) }
                                    .onSuccess { message = "Draft saved" }
                                    .onFailure { message = it.message.orEmpty() }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save draft") }
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching { repository.certify(draft) }
                                    .onSuccess { message = "Logbook entry certified" }
                                    .onFailure { message = it.message.orEmpty() }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Certify & lock") }
                }
            }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
            Text(
                "Certification records the current time and locks the entry against accidental edits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LogTextField(
    label: String,
    value: String,
    readOnly: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = !readOnly,
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun LogNumberField(
    label: String,
    value: Int,
    readOnly: Boolean,
    modifier: Modifier,
    onValueChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw -> raw.filter(Char::isDigit).toIntOrNull()?.let(onValueChange) },
        label = { Text(label) },
        enabled = !readOnly,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
