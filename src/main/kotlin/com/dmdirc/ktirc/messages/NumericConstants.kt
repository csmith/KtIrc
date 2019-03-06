package com.dmdirc.ktirc.messages

// Sources:
// ircd-seven: https://github.com/freenode/ircd-seven/blob/master/src/messages.tab
// unreal: https://github.com/unrealircd/unrealircd/blob/master/src/s_err.c

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

internal const val ERR_TOOMANYCHANNELS = "405"
internal const val ERR_NOMOTD = "422"
internal const val ERR_NONICKNAMEGIVEN = "431"
internal const val ERR_ERRONEUSNICKNAME = "432"
internal const val ERR_NICKNAMEINUSE = "433"
internal const val ERR_NICKCOLLISION = "436"
internal const val ERR_NOHIDING = "459" // Unreal
internal const val ERR_CHANNELISFULL = "471"
internal const val ERR_INVITEONLYCHAN = "473"
internal const val ERR_BANNEDFROMCHAN = "474"
internal const val ERR_BADCHANNELKEY = "475"
internal const val ERR_NEEDREGGEDNICK = "477"
internal const val ERR_BADCHANNAME = "479"
internal const val ERR_THROTTLE = "480" // ircd-seven
internal const val ERR_SECUREONLYCHAN = "489" // Unreal, inspircd
internal const val ERR_TOOMANYJOINS = "500" // Unreal, same as ERR_THROTTLE
internal const val ERR_ADMINONLY = "519" // Unreal
internal const val ERR_OPERONLY = "520" // Unreal

internal const val RPL_SASLSUCCESS = "903"
internal const val ERR_SASLFAIL = "904"
internal const val RPL_SASLMECHS = "908"
