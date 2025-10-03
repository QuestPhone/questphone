package neth.iecal.questphone.app.screens.onboard

import android.app.Application
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.backed.repositories.UserRepository
import javax.inject.Inject

open class OnboardingContent {
    // Standard title and description page
    data class StandardPage(
        val title: String,
        val description: String
    ) : OnboardingContent()

    // Custom composable content
    data class CustomPage(
        val onNextPressed: () -> Boolean = {true},
        val isNextEnabled: MutableState<Boolean> = mutableStateOf(true),
        val content: @Composable () -> Unit
    ) : OnboardingContent()
}


@HiltViewModel
class OnboarderViewModel @Inject constructor(application: Application,
  private val userRepository: UserRepository) : AndroidViewModel(application) {


    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val currentPageSp = application.applicationContext.getSharedPreferences("crnt_pg_onboard",
        Context.MODE_PRIVATE)


    private val _isNextEnabled = MutableStateFlow(true)
    val isNextEnabled: StateFlow<Boolean> = _isNextEnabled

    fun getDistractingApps(): Set<String> {
        return userRepository.getBlockedPackages()
    }
    init {
        loadCurrentPage()
    }

    fun setNextEnabled(enabled: Boolean) {
        _isNextEnabled.value = enabled
    }
    private fun loadCurrentPage(){
        _currentPage.value = currentPageSp.getInt("crnt_pg_onboard",0)
    }
    fun setCurrentPage(page: Int) {
        _currentPage.value = page
        currentPageSp.edit { putInt("crnt_pg_onboard", page) }
    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnBoarderView(
    viewModel: OnboarderViewModel,
    onFinishOnboarding: () -> Unit,
    pages: List<OnboardingContent>
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val currentPage = viewModel.currentPage.collectAsState()
    val isNextEnabled = viewModel.isNextEnabled.collectAsState()

    val isFirstPage = currentPage.value == 0
    val isLastPage = currentPage.value == pages.size - 1

    val pagerState = rememberPagerState(pageCount = { pages.size }, initialPage = currentPage.value)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setCurrentPage(page)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Horizontal Pager for swipeable pages
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { position ->
            when (val page = pages[position]) {
                is OnboardingContent.StandardPage -> {
                    StandardPageContent(
                        title = page.title,
                        description = page.description
                    )
                }

                is OnboardingContent.CustomPage -> {
                    page.content()
                }
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }

        // Back and Next buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedVisibility(
                visible = !isFirstPage,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text(
                        text = "Back",
                        fontSize = 16.sp
                    )
                }
            }

            // Spacer if no back button
            if (isFirstPage) {
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isLastPage) {
                        onFinishOnboarding()
                    } else {
                        val crnPage = pages[pagerState.currentPage]
                        if (crnPage is OnboardingContent.CustomPage) {
                            val result = crnPage.onNextPressed.invoke()
                            if (result) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                            return@Button
                        }
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                enabled = if (pages[pagerState.currentPage] is OnboardingContent.CustomPage) {
                    (pages[pagerState.currentPage] as OnboardingContent.CustomPage).isNextEnabled.value
                } else {
                    isNextEnabled.value
                }

            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

            }
        }
    }
}
@Composable
fun StandardPageContent(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = title,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )


        MarkdownText(
            markdown = description,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraLight,
                textAlign = TextAlign.Center),

        )

    }
}

