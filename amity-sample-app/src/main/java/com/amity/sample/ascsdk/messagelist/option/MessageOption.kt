package com.amity.sample.ascsdk.messagelist.option

enum class MessageOption(val value: String) {

    FLAG_MESSAGE("flag a message"),

    FLAG_SENDER("flag a sender"),

    SET_TAG("set tag(s)"),

    EDIT("edit"),

    DELETE("delete"),

    OPEN_FILE("open file"),

    ADD_REACTION("add reaction"),

    REMOVE_REACTION("remove reaction"),

    REACTION_HISTORY("reaction history");

    companion object {
        fun enumOf(value: String): MessageOption? = MessageOption.values().find { it.value == value }
    }

}