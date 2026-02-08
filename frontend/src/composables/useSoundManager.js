import { ref } from 'vue'
import { Howl } from 'howler'

const sounds = ref({})
const muted = ref(false)
const volume = ref(0.3)

export function useSoundManager() {
  function initSounds() {
    sounds.value = {
      gunshot: new Howl({
        src: ['/sounds/gunshot.mp3'],
        volume: volume.value
      }),
      play_card: new Howl({
        src: ['/sounds/play_card.mp3'],
        volume: volume.value
      }),
      draw: new Howl({
        src: ['/sounds/card_draw.mp3'],
        volume: volume.value
      }),
      card_select: new Howl({
        src: ['/sounds/card_select.mp3'],
        volume: volume.value * 0.3
      }),
      death: new Howl({
        src: ['/sounds/death.mp3'],
        volume: volume.value
      }),
      damage: new Howl({
        src: ['/sounds/damage.mp3'],
        volume: volume.value
      }),
      click: new Howl({
        src: ['/sounds/click.mp3'],
        volume: volume.value * 0.5
      })
    }
  }

  function playSound(name) {
    if (muted.value) return

    if (!sounds.value[name]) {
      // Fallback - create sound on demand
      sounds.value[name] = new Howl({
        src: [`/sounds/${name}.mp3`],
        volume: volume.value,
        onloaderror: () => {
          console.warn(`Sound not found: ${name}`)
        }
      })
    }

    sounds.value[name]?.play()
  }

  function setMuted(value) {
    muted.value = value
    Howler.mute(value)
  }

  function setVolume(value) {
    volume.value = value
    Howler.volume(value)
  }

  return {
    initSounds,
    playSound,
    setMuted,
    setVolume,
    muted,
    volume
  }
}
