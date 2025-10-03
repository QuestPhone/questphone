package neth.iecal.questphone.app.screens.quest.templates

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.backed.fetchUrlContent
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.data.CommonQuestInfo
import neth.iecal.questphone.data.Template
import neth.iecal.questphone.data.TemplateContent
import neth.iecal.questphone.data.VariableName
import neth.iecal.questphone.data.convertToTemplate
import neth.iecal.questphone.data.toAdv
import nethical.questphone.data.json
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _template = MutableStateFlow<List<Template>>(emptyList())
    val template: StateFlow<List<Template>> = _template.asStateFlow()

    val isAnonymous = userRepository.userInfo.isAnonymous
    val userCreatedOn = userRepository.userInfo.getCreatedOnString()
    val username = userRepository.userInfo.username

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _questList = MutableStateFlow<List<CommonQuestInfo>>(emptyList())
    val questList: StateFlow<List<CommonQuestInfo>> = _questList.asStateFlow()

    var selectedTemplate: Template? = null
        private set
    var selectedTemplateContent: MutableStateFlow<TemplateContent?> = MutableStateFlow<TemplateContent?>(null)
        private set
    var variableValues: MutableStateFlow<Map<String, String>> = MutableStateFlow<Map<String, String>> (mapOf<String, String>())
        private set


    val categories: StateFlow<List<String>> = template.map { list ->
        list.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredActivities: StateFlow<List<Template>> = combine(
        template, searchQuery, selectedCategory
    ) { list, query, category ->
        list.filter { activity ->
            val matchesSearch = query.isBlank() ||
                    activity.name.contains(query, ignoreCase = true) ||
                    activity.integration.label.contains(query, ignoreCase = true) ||
                    activity.description.contains(query, ignoreCase = true) ||
                    activity.category.contains(query, ignoreCase = true)

            val matchesCategory = category == null || activity.category == category

            matchesSearch && matchesCategory
        }.sortedBy{it.isFeatured}
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            loadTemplates()
            questRepository.getAllQuests().collect {
                _questList.value = it
            }
        }
    }


    fun loadTemplates() {
        viewModelScope.launch {
            val response =
                fetchUrlContent("https://raw.githubusercontent.com/QuestPhone/quest-templates/refs/heads/main/all.json")
            _template.value = parseActivitiesJson(response ?: "")
            _isLoading.value = false
        }
    }
    fun setVariable(key: String, value: String){
        variableValues.value= variableValues.value.toMutableMap().also { it[key] = value }

    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun showLoginDialog() {
        _showLoginDialog.value = true
    }

    fun hideLoginDialog() {
        _showLoginDialog.value = false
    }

    fun areAllVariablesFilled(): Boolean {
        return selectedTemplateContent.value?.questExtraVariableDeclaration?.all { variable ->
            val value = variableValues.value[variable.name]
            !value.isNullOrBlank() && value != "Not set"
        } == true
    }

    fun selectTemplate(template: Template) {
        selectedTemplate = template
        selectedTemplateContent.value = null
        variableValues.value = mutableMapOf()
        viewModelScope.launch {
            _isLoading.value = true
            val response =
                fetchUrlContent("https://raw.githubusercontent.com/QuestPhone/quest-templates/refs/heads/main/templates/${template.id}.json")
                    ?: ""
            if (response.isNotEmpty()) {
                try {
                    val data = json.decodeFromString<TemplateContent>(response)
                    selectedTemplateContent.value = data
                    VariableName.entries.forEach {
                        selectedTemplateContent.value!!.variableTypes.add(convertToTemplate(it))
                        if (selectedTemplateContent.value!!.content.contains("#{${it.name}}")) {
                            selectedTemplateContent.value!!.questExtraVariableDeclaration.add(it)
                        }
                        setVariable(it.name,it.default)
                    }
                } catch (e: Exception) {
                    Log.e("TemplateViewModel", "Error parsing JSON: ${e.message}")
                }
            }
            _isLoading.value = false
        }
    }

    fun addToQuests(onAdded: () -> Unit) {
        Log.d("vars", variableValues.toString())
        _isLoading.value = true
        selectedTemplateContent.value?.let {
            it.basicQuest.auto_destruct = variableValues.value.getOrDefault("auto_destruct", "9999-12-31")
            it.basicQuest.selected_days =
                json.decodeFromString(variableValues.value["selected_days"].toString())

            var templateExtra = it.questExtra
            Log.d("Declared Variables", it.questExtraVariableDeclaration.toString())
            it.questExtraVariableDeclaration.forEach {
                templateExtra = it.setter(templateExtra, variableValues.value, it.name)
            }
            Log.d("FInal data", templateExtra.toString())
            it.basicQuest.quest_json =
                templateExtra.getQuestJson(it.basicQuest.integration_id.toAdv())
            Log.d("Final Data", it.basicQuest.toString())
            viewModelScope.launch {
                questRepository.upsertQuest(it.basicQuest)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    onAdded()
                }
            }
        }

    }
}

private fun parseActivitiesJson(jsonString: String): List<Template>{
    return try {
        json.decodeFromString<List<Template>>(jsonString)
    } catch (e: Exception) {
        Log.e("SelectFromTemplates", "Failed to parse activities JSON", e)
        emptyList<Template>()
    }
}