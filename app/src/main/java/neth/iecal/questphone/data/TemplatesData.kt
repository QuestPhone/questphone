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
enum class VariableName(val types: VariableType,val default: String,val label : String, val setter: (AllQuestsWrapper, MutableMap<String,String>,String) -> AllQuestsWrapper = {x,_ ,_-> x }){
    selected_days(VariableType.daysOfWeek,json.encodeToString(DayOfWeek.entries.toSet()),"Which Days?"),
    auto_destruct(VariableType.date,"9999-06-21","End Date"),
    time_range(VariableType.timeRange,"[0,24]", "Time Range"),

    //AiSnap Vars
    feature1(VariableType.text, "", "Feature",{ wrapper, values,name ->
        wrapper.AiSnap.features[0] = wrapper.AiSnap.features[0].replace(name,values[name]!!)
        wrapper
    }),
    feature2(VariableType.text, "", "Feature",{ wrapper, values,name ->
        wrapper.AiSnap.features[1] = wrapper.AiSnap.features[0].replace(name,values[name]!!)
        wrapper
    }),
    taskDescription(VariableType.text,"","Task Description"),

    //Deep Focus vars
    initialFocusGoal(VariableType.number,"0","Initial Goal in Minutes",{ wrapper,values,name ->
        wrapper.DeepFocus.focusTimeConfig.initialTime = values[name]!!
        wrapper.DeepFocus.nextFocusDurationInMillis = wrapper.DeepFocus.focusTimeConfig.initialTimeInMs * 60_000
        wrapper
    }),
    incrementFocusGoalBy(VariableType.number,"0","Increment By in Minutes", {wrapper, values,name ->
        wrapper.DeepFocus.focusTimeConfig.incrementTime = values[name]!!
        wrapper
    }),
    finalFocusGoal(VariableType.number,"0","Final Goal in Hours",{wrapper, values,name ->
        wrapper.DeepFocus.focusTimeConfig.finalTime = values[name]!!
        wrapper
    }),
    unrestrictedApps(VariableType.appSelector,"[]","Unrestricted Apps",{wrapper, values,name ->
        wrapper.DeepFocus.unrestrictedApps = json.decodeFromString<Set<String>>(values[name]!!)
        wrapper
    }),

    //  Health connect vars
    initialHealthGoal(VariableType.number,"0","Initial Goal",{ wrapper,values,name ->
        wrapper.HealthConnect.healthGoalConfig.initial = values[name]!!.toInt()
        wrapper.HealthConnect.nextGoal = wrapper.HealthConnect.healthGoalConfig.initial
        wrapper
    }),
    incrementHealthGoalBy(VariableType.number,"0","Increment By", {wrapper, values,name ->
        wrapper.HealthConnect.healthGoalConfig.increment = values[name]!!.toInt()
        wrapper
    }),
    finalHealthGoal(VariableType.number,"0","Final Goal",{wrapper, values,name ->
        wrapper.HealthConnect.healthGoalConfig.final = values[name]!!.toInt()
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
        return when(type){
            IntegrationId.DEEP_FOCUS -> {
                json.encodeToString<DeepFocus>(this.DeepFocus)
            }

            IntegrationId.HEALTH_CONNECT -> {
                json.encodeToString<HealthQuest>(this.HealthConnect)
            }

            IntegrationId.SWIFT_MARK -> {
                "{}"
            }

            IntegrationId.AI_SNAP -> {
                json.encodeToString<AiSnap>(this.AiSnap)
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
