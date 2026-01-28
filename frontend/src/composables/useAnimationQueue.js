import { ref } from 'vue'
import gsap from 'gsap'

const queue = ref([])
const isProcessing = ref(false)

export function useAnimationQueue() {
  function queueAnimation(event) {
    queue.value.push(event)
    if (!isProcessing.value) {
      processQueue()
    }
  }

  async function processQueue() {
    if (queue.value.length === 0) {
      isProcessing.value = false
      return
    }

    isProcessing.value = true
    const event = queue.value.shift()

    await playAnimation(event)
    processQueue()
  }

  async function playAnimation(event) {
    return new Promise((resolve) => {
      switch (event.type) {
        case 'CARD_PLAYED':
          animateCardPlayed(event, resolve)
          break
        case 'PLAYER_DAMAGED':
          animatePlayerDamaged(event, resolve)
          break
        case 'PLAYER_ELIMINATED':
          animatePlayerEliminated(event, resolve)
          break
        default:
          resolve()
      }
    })
  }

  function animateCardPlayed(event, resolve) {
    const cardType = event.cardType
    const sourceId = event.sourcePlayerId
    const targetId = event.targetPlayerId

    // Find elements
    const sourceEl = document.querySelector(`[data-player-id="${sourceId}"]`)
    const targetEl = document.querySelector(`[data-player-id="${targetId}"]`)
    const centerEl = document.querySelector('.game-center')

    if (!sourceEl || !centerEl) {
      resolve()
      return
    }

    // Create floating card element
    const floatingCard = document.createElement('div')
    floatingCard.className = 'floating-card card-game absolute z-50'
    floatingCard.style.cssText = `
      position: fixed;
      width: 96px;
      height: 144px;
      background: linear-gradient(145deg, #E8D4A8, #D4C4A0);
      border: 3px solid #8B4513;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 14px;
      color: #2C1810;
      pointer-events: none;
    `
    floatingCard.textContent = cardType.replace('_', ' ')
    document.body.appendChild(floatingCard)

    const sourceRect = sourceEl.getBoundingClientRect()
    const centerRect = centerEl.getBoundingClientRect()

    // Start position
    gsap.set(floatingCard, {
      x: sourceRect.left + sourceRect.width / 2 - 48,
      y: sourceRect.top + sourceRect.height / 2 - 72,
      scale: 0.5,
      opacity: 0
    })

    const timeline = gsap.timeline({
      onComplete: () => {
        floatingCard.remove()
        resolve()
      }
    })

    // Animate to center
    timeline.to(floatingCard, {
      x: centerRect.left + centerRect.width / 2 - 48,
      y: centerRect.top + centerRect.height / 2 - 72,
      scale: 1,
      opacity: 1,
      duration: 0.4,
      ease: 'power2.out'
    })

    // If it's a BANG card and has a target, animate bullet
    if (cardType === 'BANG' && targetEl) {
      const targetRect = targetEl.getBoundingClientRect()

      // Create bullet element
      const bullet = document.createElement('div')
      bullet.style.cssText = `
        position: fixed;
        width: 12px;
        height: 12px;
        background: radial-gradient(circle, #FFD700, #DAA520);
        border-radius: 50%;
        box-shadow: 0 0 10px #FFD700, 0 0 20px #FFA500;
        pointer-events: none;
        z-index: 100;
      `
      document.body.appendChild(bullet)

      timeline.set(bullet, {
        x: centerRect.left + centerRect.width / 2 - 6,
        y: centerRect.top + centerRect.height / 2 - 6
      })

      timeline.to(bullet, {
        x: targetRect.left + targetRect.width / 2 - 6,
        y: targetRect.top + targetRect.height / 2 - 6,
        duration: 0.3,
        ease: 'power3.in',
        onComplete: () => {
          // Flash effect on target
          gsap.to(targetEl, {
            backgroundColor: 'rgba(255, 0, 0, 0.5)',
            duration: 0.1,
            yoyo: true,
            repeat: 3
          })
          bullet.remove()
        }
      })
    }

    timeline.to(floatingCard, {
      opacity: 0,
      scale: 0.8,
      duration: 0.3,
      delay: 0.2
    })
  }

  function animatePlayerDamaged(event, resolve) {
    const targetEl = document.querySelector(`[data-player-id="${event.targetPlayerId}"]`)

    if (!targetEl) {
      resolve()
      return
    }

    gsap.to(targetEl, {
      x: '+=10',
      duration: 0.05,
      repeat: 5,
      yoyo: true,
      ease: 'power2.inOut',
      onComplete: () => {
        gsap.set(targetEl, { x: 0 })
        resolve()
      }
    })
  }

  function animatePlayerEliminated(event, resolve) {
    const targetEl = document.querySelector(`[data-player-id="${event.targetPlayerId}"]`)

    if (!targetEl) {
      resolve()
      return
    }

    gsap.to(targetEl, {
      opacity: 0.3,
      scale: 0.9,
      filter: 'grayscale(100%)',
      duration: 0.5,
      onComplete: resolve
    })
  }

  return {
    queueAnimation,
    processQueue,
    queue,
    isProcessing
  }
}
