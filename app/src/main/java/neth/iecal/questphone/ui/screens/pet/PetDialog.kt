package neth.iecal.questphone.ui.screens.pet

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.DialogSequence
import neth.iecal.questphone.data.game.DialogStep
import neth.iecal.questphone.data.game.EventType
import neth.iecal.questphone.data.game.Pet
import neth.iecal.questphone.data.game.PetConfig
import neth.iecal.questphone.data.game.PetDialogState

@Composable
fun PetDialog(
    petId: String,
    isPetDialogVisible: MutableState<Boolean> = mutableStateOf(false),
    navController: NavHostController,
) {
    val context = LocalContext.current // Context (not used in this snippet but often useful)
    val petScript by remember { mutableStateOf(Pet.loadPetScript(petId)) }
    val dialogState = remember { Pet.petDialogState }

    var isDialogVisible by remember { mutableStateOf(true) }
    // Dismiss if pet script fails to load
    if (petScript == null) {
        LaunchedEffect(Unit) {
            isDialogVisible = false
        }
        return
    }
    // Holds the current logical step of the dialog
    var currentStep by remember { mutableStateOf<DialogStep?>(null) }
    // Holds the step whose UI (buttons, input) is currently displayed or animating out.
    // This helps prevent the animation bug by ensuring exit animations use the correct step's data.
    var displayedUiStep by remember { mutableStateOf<DialogStep?>(null) }

    var currentMessage by remember { mutableStateOf("") }
    val progress = remember { Animatable(0f) } // For text animation progress
    val animatedText by remember {
        derivedStateOf {
            currentMessage.take(progress.value.toInt())
        }
    }

    // Tracks if the current step's text animation is complete, controlling visibility of interactive elements.
    var isAnimationComplete by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }

    // Pet visual animation states
    val scale = remember { Animatable(1f) } // For pet's bounce scale
    val bounceHeight = petScript!!.personality.animations.bounceHeight
    val bounceSpeed = petScript!!.personality.animations.bounceSpeed

    val scroll = rememberScrollState() // For scrolling long text messages

    var petImage by remember { mutableStateOf("https://raw.githubusercontent.com/QuestPhone/Pets/refs/heads/main/Turtie/question.png") }


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var navigationSequences: Map<String, DialogSequence>? = emptyMap()


    // Pet bounce animation
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f + (bounceHeight * 0.1f),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (500 / bounceSpeed).toInt(),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    fun onDismiss() {
        isDialogVisible = false
    }

    // Helper function to generate the pet's spoken message, including personality.
    fun getProcessedMessage(
        step: DialogStep,
        config: PetConfig,
        currentDialogState: PetDialogState
    ): String {
        var processedMessage = step.message
        // Replace placeholders with user data
        currentDialogState.userData.forEach { (key, value) ->
            processedMessage = processedMessage.replace("{$key}", value)
        }

        val style = config.personality.speakingStyle

        // Randomly add a catchphrase
        if (style.catchphrases.isNotEmpty() && Math.random() < 0.3) {
            val catchphrase = style.catchphrases.random()
            processedMessage += " $catchphrase"
        }

        // Randomly add an emoji
        if (style.emojis.isNotEmpty() && Math.random() < 0.4) {
            val emoji = style.emojis.random()
            processedMessage += " $emoji"
        }

        // Apply pet's typical punctuation style
        if (style.punctuation.isNotEmpty() && !processedMessage.endsWith(style.punctuation)) {
            // This logic aims to replace common sentence-ending punctuation or append the style.
            processedMessage = processedMessage.replace(".", style.punctuation)
                .replace("!", style.punctuation)
                .replace(
                    "?",
                    "?${style.punctuation.replace(".", "")}"
                ) // Handles question marks specially
        }
        return processedMessage
    }


    fun processNextStep() {
        val config = petScript ?: return onDismiss() // Dismiss if script is somehow null now

        var effectiveSequenceId = dialogState.currentSequenceId
        // Tentatively move to next step index. If currentStepIndex is -1 (new sequence), this becomes 0.
        var effectiveStepIndex = dialogState.currentStepIndex + 1

        var currentSequence = config.dialogSequences[effectiveSequenceId]

        // Handle initial state (no sequenceId yet) or invalid sequenceId
        if (currentSequence == null) {
            effectiveSequenceId = config.defaultSequence
            currentSequence = config.dialogSequences[effectiveSequenceId]
            if (currentSequence == null) {
                Log.e(
                    "PetDialog",
                    "Default sequence '${config.defaultSequence}' not found. Dismissing."
                )
                onDismiss() // No valid sequence to run
                return
            }
            effectiveStepIndex = 0 // Start at the beginning of the default sequence
        }

        // Check if end of current sequence is reached
        if (effectiveStepIndex >= currentSequence.steps.size) {
            val nextSequenceFromSequenceEnd = currentSequence.nextSequence
            if (nextSequenceFromSequenceEnd != null && config.dialogSequences.containsKey(
                    nextSequenceFromSequenceEnd
                )
            ) {
                // Transition to the next sequence defined by the current sequence's 'nextSequence' field
                effectiveSequenceId = nextSequenceFromSequenceEnd
                currentSequence =
                    config.dialogSequences[effectiveSequenceId]!! // Should exist due to containsKey
                effectiveStepIndex = 0 // Start at the beginning of the new sequence
            } else {
                // No next sequence defined, or it's invalid; dialog ends
                onDismiss()
                return
            }
        }

        // Update the dialog state
        dialogState.currentSequenceId = effectiveSequenceId
        dialogState.currentStepIndex = effectiveStepIndex

        val step = currentSequence.steps.getOrNull(effectiveStepIndex)
        if (step == null) {
            Log.e(
                "PetDialog",
                "Step at index $effectiveStepIndex in sequence '$effectiveSequenceId' is null. Dismissing."
            )
            onDismiss() // Should not happen if logic is correct and JSON is valid
            return
        }

        // Conditions are commented out in your original code. If re-enabled,
        // they would need careful integration here, potentially changing step or sequence
        // and requiring a re-fetch or recursive call.

        currentStep = step // Update currentStep to the new one
        Pet.saveCurrentStep()
        
        currentMessage =
            getProcessedMessage(currentStep!!, config, dialogState) // Pass non-null currentStep
        userInput = ""
    }

    // Inside PetDialog composable
    fun handleInputSubmission(input: String = userInput) {
        val stepBeingHandled =
            currentStep ?: return // The step whose input/choice we are processing

        // Store user input if the step expects it
        if (stepBeingHandled.expectsInput && stepBeingHandled.inputKey != null) {
            dialogState.userData[stepBeingHandled.inputKey] =
                input.ifBlank { "User" } // Default to "User" if blank
        }

        // Handle choices, if any
        if (stepBeingHandled.choices.isNotEmpty()) {
            val selectedChoice = stepBeingHandled.choices.find { it.text == input }
            if (selectedChoice != null) {
                // Store the chosen value if specified
                if (selectedChoice.storeValue != null ) {
                    dialogState.userData[selectedChoice.storeValue[0]] = selectedChoice.storeValue[1]
                }

                // If choice dictates a jump to a specific next sequence ID
                if (selectedChoice.nextStepId != null) {
                    val config = petScript ?: return
                    if (config.dialogSequences.containsKey(selectedChoice.nextStepId)) {
                        dialogState.currentSequenceId =
                            selectedChoice.nextStepId!! // !! safe due to null check
                        dialogState.currentStepIndex =
                            -1 // processNextStep will increment this to 0 for the new sequence
                        // REMOVED 'return' here: Allow processNextStep() to be called below
                    } else {
                        // Optional: Log a warning if the nextStepId is not a valid sequence ID
                        Log.w(
                            "PetDialog",
                            "Choice's nextStepId '${selectedChoice.nextStepId}' is not a valid sequence ID. Proceeding to next step in current sequence."
                        )
                        onDismiss()
                        return

                    }
                }
            }
        }
        processNextStep() // This will now be called, initiating the new sequence or the next step.
    }
    fun jumpToSequence(sequenceId: String) {
        val config = petScript ?: return onDismiss()
        if (!config.dialogSequences.containsKey(sequenceId)) {
            Log.e("PetDialog", "Sequence '$sequenceId' not found. Dismissing.")
            return onDismiss()
        }

        // Reset state
        dialogState.currentSequenceId = sequenceId
        dialogState.currentStepIndex = -1 // Will be incremented to 0 by processNextStep
        isDialogVisible = true
        processNextStep()
    }

    LaunchedEffect(currentRoute) {
        println("Currently on route: $currentRoute")

        val sequences = petScript?.dialogSequences?.filter {
            it.value.eventType == EventType.NAVIGATION &&
                    it.value.eventData == currentRoute
        }
        val matchingKeys = sequences?.entries
            ?.filter { (_, sequence) ->
                Log.d("mat",sequence.toString())
                sequence.conditions.all { (key, value) ->
                    dialogState.userData.getOrDefault(key, "false") == value
                }
            }
            ?.map { it.key }

        Log.d("matching sequences", matchingKeys.toString())
        Log.d("User Data", dialogState.userData.toString())

        if(matchingKeys?.isNotEmpty() == true) jumpToSequence(matchingKeys[0])
        // You can trigger other effects or UI changes here
    }

    // Effect for animating text character by character
    LaunchedEffect(currentMessage) {
        if (currentMessage.isNotEmpty()) {
            petImage =
                "https://raw.githubusercontent.com/QuestPhone/Pets/refs/heads/main/Turtie/${currentStep!!.emotion}.png"
            val typingSpeed = petScript?.personality?.speakingStyle?.typingSpeed ?: 1f
            progress.snapTo(0f) // Reset progress
            isAnimationComplete = false // Signal that text animation is starting

            progress.animateTo(
                targetValue = currentMessage.length.toFloat(),
                animationSpec = tween(
                    durationMillis = (currentMessage.length * 50 / typingSpeed).toInt()
                        .coerceAtLeast(50),
                    easing = LinearEasing
                )
            )
            // Text animation is complete
            isAnimationComplete = true
            displayedUiStep = currentStep // Update the step for UI display now that text is ready

        } else {
            // If currentMessage is empty (e.g. start or an error), ensure states are consistent
            isAnimationComplete = true // Or false, depending on desired state for empty messages
            displayedUiStep = currentStep
        }
    }
    LaunchedEffect(scroll.maxValue) {
        if (scroll.canScrollForward) { // Check if there's content to scroll towards the end
            scroll.animateScrollTo(scroll.maxValue)
            // }
        }
    }

// Inside PetDialog composable
// Add import: import android.util.Log

    LaunchedEffect(petScript) {
        val script = petScript
            ?: return@LaunchedEffect // If script is null, do nothing yet. Dismissal is in Dialog's top check.

        var needsProcessing = false
        var resetToDefaultSequence = false

        if (currentStep == null) { // Always process if currentStep isn't set (e.g., first launch)
            needsProcessing = true
        }

        // Validate current dialog state's sequenceId against the loaded script
        if (!script.dialogSequences.containsKey(dialogState.currentSequenceId)) {
            Log.w(
                "PetDialog",
                "Saved sequenceId '${dialogState.currentSequenceId}' not found in script '${script.id}'. Resetting to default sequence '${script.defaultSequence}'."
            )
            resetToDefaultSequence = true
        } else {
            // Sequence ID is valid, check if step index is plausible.
            val loadedSequence = script.dialogSequences[dialogState.currentSequenceId]
            if (loadedSequence == null || dialogState.currentStepIndex >= loadedSequence.steps.size) {
                Log.w(
                    "PetDialog",
                    "Saved stepIndex '${dialogState.currentStepIndex}' is out of bounds for sequence '${dialogState.currentSequenceId}'. Resetting to start of sequence."
                )
                dialogState.currentStepIndex = -1 // Will be incremented to 0 by processNextStep
                needsProcessing = true
            } else if (dialogState.currentStepIndex < -1) { // Ensure step index is not an invalid negative number
                Log.w(
                    "PetDialog",
                    "Saved stepIndex '${dialogState.currentStepIndex}' is invalid. Resetting to start of sequence."
                )
                dialogState.currentStepIndex = -1
                needsProcessing = true
            }
        }

        if (resetToDefaultSequence) {
            dialogState.currentSequenceId = script.defaultSequence
            dialogState.currentStepIndex = -1 // Prime for processNextStep to start at 0
            // Verify the default sequence itself exists
            if (!script.dialogSequences.containsKey(dialogState.currentSequenceId)) {
                Log.e(
                    "PetDialog",
                    "FATAL: Default sequence '${dialogState.currentSequenceId}' for pet '${script.id}' not found in script. Cannot proceed."
                )
                onDismiss()
                return@LaunchedEffect
            }
            needsProcessing = true
        }

        if (needsProcessing) {
            isAnimationComplete =
                false // Hide interactive elements before the first/new message animates
            processNextStep()
        }
        // If currentStep is already set and valid for the script, existing state is fine.
    }
    if (isDialogVisible) {
        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false, // User must interact with dialog
                usePlatformDefaultWidth = false // Use custom width from modifier
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter // Align card to bottom
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp) // Padding to avoid overlap with navigation bar
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .navigationBarsPadding()
                            .fillMaxWidth()
                    ) {
                        // Section for Pet Image and Dialog Text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {

                            Image(
                                painter = rememberAsyncImagePainter(

                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(
                                            petImage
                                        )
                                        .crossfade(true)
                                        .error(R.drawable.baseline_person_24)
                                        .build(),
                                ),
                                contentDescription = "Pet Image",
                                modifier = Modifier
                                    .size(120.dp)
                                    .scale(scale.value) // Apply bounce animation
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    // Fixed height for text area, might leave some white space
                                    .height(135.dp)
                                    .verticalScroll(scroll) // Scroll if text overflows
                            ) {
                                Text(
                                    text = animatedText, // Display animated text
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        // Divider, visible when text animation is complete
                        AnimatedVisibility(
                            visible = isAnimationComplete,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 400,
                                    delayMillis = 200
                                )
                            ) +
                                    slideInVertically(
                                        animationSpec = tween(
                                            durationMillis = 400,
                                            delayMillis = 200
                                        ),
                                        initialOffsetY = { -it / 2 }
                                    ),
                            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }

                        // Interactive section (choices, input, continue button)
                        // This section is also visible only after text animation completes.
                        // Content uses displayedUiStep to ensure correct data during animations.
                        AnimatedVisibility(
                            visible = isAnimationComplete,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    delayMillis = 300
                                )
                            ) +
                                    slideInVertically(
                                        animationSpec = tween(
                                            durationMillis = 500,
                                            delayMillis = 300
                                        ),
                                        initialOffsetY = { it / 3 }
                                    ) +
                                    expandVertically(
                                        animationSpec = tween(
                                            durationMillis = 500,
                                            delayMillis = 300
                                        )
                                    ),
                            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically() + shrinkVertically()
                        ) {
                            // Determine what to show based on the displayedUiStep
                            when {
                                // Display choice buttons if the step has choices
                                displayedUiStep?.choices?.isNotEmpty() == true -> {
                                    LazyColumn(
                                        modifier = Modifier.heightIn(max = 200.dp), // Max height for choice list
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(
                                            displayedUiStep?.choices ?: emptyList()
                                        ) { index, choice ->
                                            // Animation for individual button appearance (staggered)
                                            val animatedModifierFactor by animateFloatAsState(
                                                targetValue = 1f, // Animate to full visibility/scale
                                                animationSpec = tween(
                                                    durationMillis = 300,
                                                    delayMillis = index * 100 // Staggered delay
                                                ),
                                                label = "choice_button_animation"
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    isAnimationComplete =
                                                        false // Hide interactive elements for transition
                                                    handleInputSubmission(choice.text)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .scale(animatedModifierFactor)
                                                    .alpha(animatedModifierFactor),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                )
                                            ) {
                                                Text(
                                                    choice.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Display input field if the step expects text input
                                displayedUiStep?.expectsInput == true -> {
                                    val slideOffset by animateIntAsState(
                                        targetValue = 0,
                                        animationSpec = tween(durationMillis = 400),
                                        label = "input_slide_offset"
                                    )
                                    val fadeAlpha by animateFloatAsState(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 600),
                                        label = "input_fade_alpha"
                                    )

                                    Column(
                                        modifier = Modifier
                                            .offset(y = slideOffset.dp)
                                            .alpha(fadeAlpha)
                                    ) {
                                        OutlinedTextField(
                                            value = userInput,
                                            onValueChange = { userInput = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text(
                                                    "Your response",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                                    alpha = 0.5f
                                                )
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            val buttonScale by animateFloatAsState(
                                                targetValue = 1f,
                                                animationSpec = tween(
                                                    durationMillis = 300,
                                                    delayMillis = 200
                                                ),
                                                label = "submit_button_scale"
                                            )
                                            Button(
                                                onClick = {
                                                    isAnimationComplete =
                                                        false // Hide for transition
                                                    handleInputSubmission() // Uses current userInput
                                                },
                                                modifier = Modifier.scale(buttonScale),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text(
                                                    "Continue",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Display a simple "Continue" button otherwise
                                else -> {
                                    val bounceScale by animateFloatAsState(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "continue_button_scale"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                isAnimationComplete = false // Hide for transition
                                                processNextStep()
                                            },
                                            modifier = Modifier.scale(bounceScale),
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text(
                                                "Continue",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Spacer(modifier = Modifier.width(4.dp)) // Original spacer, check if icon was intended
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
