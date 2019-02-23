package com.dmdirc.ktirc.messages

internal const val CTCP_BYTE : Byte = 1

internal const val RPL_WELCOME = "001"
internal const val RPL_ISUPPORT = "005"

internal const val RPL_UMODEIS = "221"

internal const val RPL_CHANNELMODEIS = "324"
internal const val RPL_NOTOPIC = "331"
internal const val RPL_TOPIC = "332"
internal const val RPL_TOPICWHOTIME = "333"
internal const val RPL_MOTD = "372"
internal const val RPL_MOTDSTART = "375"
internal const val RPL_ENDOFMOTD = "376"

internal const val ERR_NOMOTD = "422"
internal const val ERR_NONICKNAMEGIVEN = "431"
internal const val ERR_ERRONEUSNICKNAME = "432"
internal const val ERR_NICKNAMEINUSE = "433"
internal const val ERR_NICKCOLLISION = "436"

internal const val RPL_SASLSUCCESS = "903"
internal const val ERR_SASLFAIL = "904"
internal const val RPL_SASLMECHS = "908"
