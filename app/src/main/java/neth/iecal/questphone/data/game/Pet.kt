package neth.iecal.questphone.data.game

data class StoryNode(
    val text: String,
    val options: List<StoryOption>
)

data class StoryOption(
    val text: String,
    val nextNodeId: String?
)


val introductionStory = mapOf<String,StoryNode>(
    "welcome" to StoryNode("**Congratulations on becoming a Player.**", listOf(StoryOption("Next","introduction"))),
    "introduction" to StoryNode(
        "This is **System**.\nNot here to judge.\nNot here to hype you up.\nI’m here to help you rebuild—step by step.",
        listOf(StoryOption("Next", "introduction_2"))
    ),
    "introduction_2" to StoryNode(
        "You don’t need more motivation.\nYou need a plan.\nYou don’t need comfort.\nYou need change.\nAnd it starts now.",
        listOf(StoryOption("Next", "introduction_3"))
    ),
    "introduction_3" to StoryNode(
        "I’ll guide you.\nHold you steady.\nHelp you grow.\nYour habits? We fix them.\nYour mindset? We sharpen it.",
        listOf(StoryOption("Next", "introduction_4"))
    ),
    "introduction_4" to StoryNode(
        "This is your turning point.\nYou play, or stay stuck.\n**Welcome to Level 1.**",
        listOf(
            StoryOption("Explore Application", "explore"),
            StoryOption("Close", null)
        )
    ),

    )