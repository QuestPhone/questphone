package neth.iecal.questphone.data

import kotlinx.serialization.Serializable
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.ai.snap.AiSnap
import neth.iecal.questphone.data.quest.focus.DeepFocus
import neth.iecal.questphone.data.quest.health.HealthQuest
import neth.iecal.questphone.utils.json

@Serializable
enum class VariableType{
    daysOfWeek,date,timeRange,text,number,appSelector
}

@Serializable
enum class VariableName(val types: VariableType,val default: String,val label : String, val setter: (AllQuestsWrapper, MutableMap<String,String>) -> AllQuestsWrapper = {x,_ -> x }){
    selected_days(VariableType.daysOfWeek,json.encodeToString(DayOfWeek.entries.toSet()),"Which Days?"),
    auto_destruct(VariableType.date,"9999-06-21","End Date"),
    time_range(VariableType.timeRange,"[0,24]", "Time Range"),

    features(VariableType.text, "[]", "Features"),
    taskDescription(VariableType.text,"","Task Description"),

    initialGoal(VariableType.number,"0","Initial Goal in Minutes",{ wrapper,values ->
        wrapper.DeepFocus.focusTimeConfig.initialTime = values.getOrDefault("initialGoal","")
        wrapper.DeepFocus.nextFocusDurationInMillis = wrapper.DeepFocus.focusTimeConfig.initialTimeInMs * 60_000
        wrapper
    }),
    incrementGoalBy(VariableType.number,"0","Increment By in Minutes", {wrapper, values ->
        wrapper.DeepFocus.focusTimeConfig.incrementTime = values.getOrDefault("incrementGoalBy","")
        wrapper
    }),
    finalGoal(VariableType.number,"0","Final Goal in Minutes",{wrapper, values ->
        wrapper.DeepFocus.focusTimeConfig.finalTime = values.getOrDefault("incrementGoalBy","")
        wrapper
    }),
    unrestrictedApps(VariableType.appSelector,"[]","Unrestricted Apps",{wrapper, values ->
        wrapper.DeepFocus.unrestrictedApps = json.decodeFromString<Set<String>>(values.getOrDefault("unrestrictedApps","[]"))
        wrapper
    }),

}
@Serializable
data class TemplateVariable(
    val name: String,
    val type: VariableType,
    val label: String,
    val default: String? = null
){
    fun getDefaultValue():String{
        if(default!= null) return default
        return when(type) {
            VariableType.daysOfWeek -> json.encodeToString(DayOfWeek.entries.toSet())
            VariableType.date -> "9999-06-21"
            VariableType.timeRange -> "[0,24]"
            VariableType.text -> label
            VariableType.number -> "0"
            VariableType.appSelector -> "[]"
        }
    }
}
fun convertToTemplate(variable: VariableName): TemplateVariable {
    return TemplateVariable(variable.name,variable.types,variable.label,variable.default)
}
@Serializable
data class AllQuestsWrapper(
    val AiSnap: AiSnap = AiSnap(),
    val DeepFocus: DeepFocus = DeepFocus(),
    val HealthConnect: HealthQuest = HealthQuest(),
) {
    fun getQuestJson(type: IntegrationId):String{
        when(type){
            IntegrationId.DEEP_FOCUS -> {
                json.encodeToString<DeepFocus>(DeepFocus)
            }
            IntegrationId.HEALTH_CONNECT -> {
                json.encodeToString<HealthQuest>(HealthConnect)
            }
            IntegrationId.SWIFT_MARK -> {
                "{}"
            }
            IntegrationId.AI_SNAP -> {
                json.encodeToString<AiSnap>(AiSnap)
            }
        }
        return "{}"
    }
}
@Serializable
data class TemplateData(
    val content: String,
    val variableTypes: MutableList<TemplateVariable> = mutableListOf(),
    val questExtraVariableDeclaration: MutableList<VariableName> = mutableListOf(),
    val requirements: String,
    val basicQuest: CommonQuestInfo = CommonQuestInfo(),
    val questExtra: AllQuestsWrapper = AllQuestsWrapper()
)
