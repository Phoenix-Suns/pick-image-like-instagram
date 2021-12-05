package self.tranluunghia.selectimage.utils

object TimeUtils {
    /**
     * convert Millisecond to hh:mm:ss
     *
     * @param millis
     * @return
     */
    fun toMinuteSecond(millis: Long): String {
        val seconds = millis / 1000 % 60
        val minute = millis / (1000 * 60) // Max 60 minute Per Hour
        return String.format("%02d:%02d", minute, seconds)
    }
}