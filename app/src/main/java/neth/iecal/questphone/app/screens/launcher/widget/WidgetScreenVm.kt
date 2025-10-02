package neth.iecal.questphone.app.screens.launcher.widget.picker

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.backed.repositories.AppWidgetConfig
import neth.iecal.questphone.backed.repositories.WidgetModule

data class PersistedWidget(
    val widgetId: Int,
    val widgetInfo: AppWidgetProviderInfo,
    val config: AppWidgetConfig
)

data class WidgetUiState(
    val widgets: List<PersistedWidget> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class WidgetViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WidgetUiState())
    val uiState: StateFlow<WidgetUiState> = _uiState.asStateFlow()

    val repository = WidgetModule.provideAppWidgetConfigDao(WidgetModule.provideDatabase(application))
    private val appWidgetManager = AppWidgetManager.getInstance(application)

    init {
        loadPersistedWidgets()
    }

    fun loadPersistedWidgets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val configs = repository.getAllConfigs()
                val widgets = mutableListOf<PersistedWidget>()

                configs.forEach { config ->
                    // Verify widget still exists and is valid
                    val widgetInfo = appWidgetManager.getAppWidgetInfo(config.widgetId)
                    if (widgetInfo != null) {
                        widgets.add(
                            PersistedWidget(
                                widgetId = config.widgetId,
                                widgetInfo = widgetInfo,
                                config = config
                            )
                        )
                    } else {
                        // Widget no longer exists, remove from database
                        repository.deleteConfig(config)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    widgets = widgets,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load widgets: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun addWidget(widgetId: Int, widgetInfo: AppWidgetProviderInfo) {
        viewModelScope.launch {
            try {
                val currentOrder = _uiState.value.widgets.size
                val config = AppWidgetConfig(
                    id = "${widgetInfo.provider.packageName}_${widgetInfo.provider.className}_$widgetId",
                    widgetId = widgetId,
                    height = widgetInfo.minHeight,
                    width = widgetInfo.minWidth,
                    borderless = false,
                    background = true,
                    themeColors = true,
                    order = currentOrder // 👈 put it last
                )
                repository.insertConfig(config)

                val persistedWidget = PersistedWidget(widgetId, widgetInfo, config)
                _uiState.value = _uiState.value.copy(
                    widgets = _uiState.value.widgets + persistedWidget
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to add widget: ${e.message}")
            }
        }
    }

    fun removeWidget(widgetId: Int) {
        viewModelScope.launch {
            try {
                val widget = _uiState.value.widgets.find { it.widgetId == widgetId }
                if (widget != null) {
                    repository.deleteConfig(widget.config)
                    _uiState.value = _uiState.value.copy(
                        widgets = _uiState.value.widgets.filter { it.widgetId != widgetId }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove widget: ${e.message}"
                )
            }
        }
    }

    fun updateWidgetConfig(widgetId: Int, updatedConfig: AppWidgetConfig) {
        viewModelScope.launch {
            try {
                repository.insertConfig(updatedConfig)

                _uiState.value = _uiState.value.copy(
                    widgets = _uiState.value.widgets.map { widget ->
                        if (widget.widgetId == widgetId) {
                            widget.copy(config = updatedConfig)
                        } else {
                            widget
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update widget config: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearAllWidgets() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(widgets = emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to clear widgets: ${e.message}"
                )
            }
        }
    }
    fun reorderWidgets(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                val currentWidgets = _uiState.value.widgets.toMutableList()
                if (fromIndex in currentWidgets.indices && toIndex in currentWidgets.indices) {
                    val widget = currentWidgets.removeAt(fromIndex)
                    currentWidgets.add(toIndex, widget)

                    // update state
                    _uiState.value = _uiState.value.copy(widgets = currentWidgets)

                    // update DB order values
                    currentWidgets.forEachIndexed { index, persistedWidget ->
                        val updatedConfig = persistedWidget.config.copy(order = index)
                        repository.insertConfig(updatedConfig)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to reorder widgets: ${e.message}"
                )
            }
        }
    }

}