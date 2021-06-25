package dev.jonaz.vured.bot.service.web

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.jonaz.vured.bot.persistence.web.PlayerEvent
import dev.jonaz.vured.bot.persistence.web.PlayerEventQueueItem
import dev.jonaz.vured.bot.service.music.MusicService
import dev.jonaz.vured.util.extensions.genericInject
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.BlockingQueue

class PlayerService {
    private val musicService by genericInject<MusicService>()

    val events = MutableSharedFlow<PlayerEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun sendEvent(audioPlayer: AudioPlayer) = runBlocking {
        val queue = musicService.getQueue()
        val playerEvent = mapEventFromAudioPlayer(audioPlayer)

        mapQueueToPlayerEvent(queue, playerEvent).run {
            events.emit(this)
        }
    }

    fun getEvent(): PlayerEvent = musicService.getAudioPlayer().run {
        val queue = musicService.getQueue()
        val playerEvent = mapEventFromAudioPlayer(this)

        return mapQueueToPlayerEvent(queue, playerEvent)
    }

    fun convertEventToFrame(event: PlayerEvent): Frame {
        val eventByteArray = Json.encodeToString(event).toByteArray()

        return Frame.byType(
            fin = true,
            frameType = FrameType.BINARY,
            data = eventByteArray
        )
    }

    private fun mapEventFromAudioPlayer(audioPlayer: AudioPlayer) = PlayerEvent(
        isPaused = audioPlayer.isPaused,
        volume = audioPlayer.volume,
        title = audioPlayer.playingTrack?.info?.title,
        author = audioPlayer.playingTrack?.info?.author,
        isStream = audioPlayer.playingTrack?.info?.isStream,
        uri = audioPlayer.playingTrack?.info?.uri,
        duration = audioPlayer.playingTrack?.duration,
        position = audioPlayer.playingTrack?.position,
        identifier = audioPlayer.playingTrack?.identifier
    )

    private fun mapQueueToPlayerEvent(queue: BlockingQueue<AudioTrack>, event: PlayerEvent): PlayerEvent {
        event.queue = queue.map {
            PlayerEventQueueItem(
                title = it.info.title,
                uri = it.info.uri,
                identifier = it.identifier
            )
        }

        return event
    }
}
