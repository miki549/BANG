import { ref } from 'vue'
import gsap from 'gsap'
import { useSoundManager } from './useSoundManager'

const queue = ref([])
const isProcessing = ref(false)
const animatingCardIds = ref(new Set())

export function useAnimationQueue() {
  const { playSound } = useSoundManager()

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
    if (event.cardId) {
        animatingCardIds.value.add(event.cardId)
    }
    return new Promise((resolve) => {
      const onResolve = () => {
          if (event.cardId) {
              animatingCardIds.value.delete(event.cardId)
          }
          resolve()
      }
      switch (event.type) {
        case 'CARD_PLAYED':
          animateCardPlayed(event, onResolve)
          break
        case 'PLAYER_DAMAGED':
          animatePlayerDamaged(event, onResolve)
          break
        case 'PLAYER_ELIMINATED':
          animatePlayerEliminated(event, onResolve)
          break
        case 'CARD_DRAWN':
          animateCardDrawn(event, onResolve)
          break
        case 'CARD_STOLEN':
          animateCardStolen(event, onResolve)
          break
        case 'CARD_DISCARDED':
          animateCardDiscarded(event, onResolve)
          break
        case 'CARD_CHECK':
          animateCardCheck(event, onResolve)
          break
        case 'CARD_PASSED':
          animateCardPassed(event, onResolve)
          break
        case 'STATE_UPDATE':
          if (event.updateFn) event.updateFn()
          onResolve()
          break
        default:
          onResolve()
      }
    })
  }

  function getSuitSymbol(suit) {
    const suits = {
      'HEARTS': '♥',
      'DIAMONDS': '♦',
      'CLUBS': '♣',
      'SPADES': '♠'
    }
    return suits[suit] || '?'
  }

  function getCardImage(type) {
    if (!type || type === 'UNKNOWN') return '/images/common/deck.png'
    const imageMap = {
      'BANG': 'bang.png',
      'MISSED': 'missed.png',
      'BEER': 'beer.png',
      'SALOON': 'saloon.png',
      'STAGECOACH': 'stagecoach.png',
      'WELLS_FARGO': 'wellsfargo.png',
      'PANIC': 'panic.png',
      'CAT_BALOU': 'catbalou.png',
      'DUEL': 'duel.png',
      'GATLING': 'gatling.png',
      'INDIANS': 'indians.png',
      'GENERAL_STORE': 'generalstore.png',
      'BARREL': 'barrel.png',
      'MUSTANG': 'mustang.png',
      'SCOPE': 'scope.png',
      'JAIL': 'jail.png',
      'DYNAMITE': 'dinamite.png',
      'VOLCANIC': 'volcanic.png',
      'SCHOFIELD': 'schofield.png',
      'REMINGTON': 'remington.png',
      'REV_CARABINE': 'carabine.png',
      'WINCHESTER': 'winchester.png'
    }
    const filename = imageMap[type] || 'bang.png'
    return `/images/cards/${filename}`
  }

  function isBlueCard(type) {
    const blueCards = [
      'VOLCANIC', 'SCHOFIELD', 'REMINGTON', 'REV_CARABINE', 'WINCHESTER',
      'BARREL', 'SCOPE', 'MUSTANG', 'JAIL', 'DYNAMITE'
    ]
    return blueCards.includes(type)
  }

  function createFloatingCard(type, x, y) {
    const el = document.createElement('div')
    el.className = 'fixed z-[100] shadow-2xl rounded-lg overflow-hidden'
    el.style.cssText = `
      position: fixed;
      left: 0;
      top: 0;
      width: 96px;
      height: 144px;
      transform: translate(${x}px, ${y}px);
      pointer-events: none;
    `
    
    const img = document.createElement('img')
    img.className = 'w-full h-full object-cover'
    
    if (!type || type === 'UNKNOWN' || type === 'BACK') {
        // Use card back style or image
        el.classList.add('card-game', 'card-back')
    } else {
        img.src = getCardImage(type)
        el.appendChild(img)
    }
    
    document.body.appendChild(el)
    return el
  }

  function animateCardDrawn(event, resolve) {
    const deckEl = document.querySelector('[data-deck-pile]')
    const targetEl = document.querySelector(`[data-player-id="${event.targetPlayerId}"]`)

    if (!deckEl || !targetEl) {
      resolve()
      return
    }

    const startRect = deckEl.getBoundingClientRect()
    const endRect = targetEl.getBoundingClientRect()

    const card = createFloatingCard('BACK', startRect.left, startRect.top)
    
    // Slight random offset for "stack" feel
    const randomRot = (Math.random() - 0.5) * 20
    
    // Play sound at start of animation
    playSound('draw')

    gsap.to(card, {
        x: endRect.left + endRect.width / 2 - 48,
        y: endRect.top + endRect.height / 2 - 72,
        rotation: randomRot,
        duration: 0.6,
        ease: 'power2.inOut',
        onComplete: () => {
            card.remove()
            resolve()
        }
    })
  }

  function animateCardStolen(event, resolve) {
    const sourceEl = document.querySelector(`[data-player-id="${event.sourcePlayerId}"]`)
    const targetEl = document.querySelector(`[data-player-id="${event.targetPlayerId}"]`)

    if (!sourceEl || !targetEl) {
      resolve()
      return
    }

    const startRect = sourceEl.getBoundingClientRect()
    const endRect = targetEl.getBoundingClientRect()

    // Stealing is usually face down
    const card = createFloatingCard('BACK', startRect.left + startRect.width/2 - 48, startRect.top + startRect.height/2 - 72)

    // Play sound for stealing (draw sound is appropriate)
    playSound('draw')

    gsap.to(card, {
        x: endRect.left + endRect.width / 2 - 48,
        y: endRect.top + endRect.height / 2 - 72,
        rotation: 360, // Spin effect for stealing
        scale: 1.1,
        duration: 0.7,
        ease: 'back.inOut(1.2)',
        onComplete: () => {
            card.remove()
            resolve()
        }
    })
  }

  function animateCardPassed(event, resolve) {
    // Similar to Discard but moves to another player's blue slot area
    const sourceEl = document.querySelector(`[data-player-id="${event.sourcePlayerId}"]`)
    const targetEl = document.querySelector(`[data-player-id="${event.targetPlayerId}"]`)

    if (!sourceEl || !targetEl) {
      resolve()
      return
    }

    const startRect = sourceEl.getBoundingClientRect()
    const endRect = targetEl.getBoundingClientRect()

    // Create card with specific type (e.g. DYNAMITE)
    const card = createFloatingCard(event.cardType, startRect.left + startRect.width/2 - 48, startRect.top + startRect.height/2 - 72)
    
    playSound('play_card')

    // Animate arc to next player
    gsap.to(card, {
        x: endRect.left + endRect.width / 2 - 48,
        y: endRect.top + endRect.height / 2 - 72,
        rotation: 360,
        scale: 1, // Keep normal size
        duration: 0.8,
        ease: 'power2.inOut',
        onComplete: () => {
            card.remove()
            resolve()
        }
    })
  }

  function animateCardDiscarded(event, resolve) {
    const sourceEl = document.querySelector(`[data-player-id="${event.sourcePlayerId}"]`)
    const discardEl = document.querySelector('[data-discard-pile]')

    if (!sourceEl || !discardEl) {
      resolve()
      return
    }

    const startRect = sourceEl.getBoundingClientRect()
    const endRect = discardEl.getBoundingClientRect()
    
    // Discard is face up
    const card = createFloatingCard(event.cardType, startRect.left + startRect.width/2 - 48, startRect.top + startRect.height/2 - 72)

    playSound('play_card')

    gsap.to(card, {
        x: endRect.left + endRect.width / 2 - 48,
        y: endRect.top + endRect.height / 2 - 72,
        rotation: (Math.random() - 0.5) * 30,
        scale: 1,
        duration: 0.5,
        ease: 'power2.out',
        onComplete: () => {
            card.remove()
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

    // Create floating card element using new helper
    // We want to show the specific card image now
    const sourceRect = sourceEl.getBoundingClientRect()
    const centerRect = centerEl.getBoundingClientRect()
    
    const floatingCard = createFloatingCard(cardType, sourceRect.left + sourceRect.width/2 - 48, sourceRect.top + sourceRect.height/2 - 72)
    
    // Initial state
    gsap.set(floatingCard, {
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
      scale: 1.5, // Slightly larger when played
      opacity: 1,
      duration: 0.5,
      ease: 'back.out(1.7)'
    })
    
    // Add label for synchronization
    timeline.addLabel('atCenter')

    // If it's a BANG card and has a target, animate bullet concurrently with pause/next steps
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

      // Bullet starts at center (needs to be set immediately in timeline context)
      // We use .call to ensure DOM element exists if we created it inside timeline,
      // but here we created it outside.
      // We want it to appear exactly when card hits center.
      
      timeline.set(bullet, {
        x: centerRect.left + centerRect.width / 2 - 6,
        y: centerRect.top + centerRect.height / 2 - 6,
        opacity: 1
      }, 'atCenter')

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
      }, 'atCenter+=0.1') // Fire slightly after reaching center
    }

    if (isBlueCard(cardType)) {
        // For blue cards, move to player board after center
        const destEl = (cardType === 'JAIL' && targetEl) ? targetEl : sourceEl
        const destRect = destEl.getBoundingClientRect()
        
        timeline.to(floatingCard, {
            x: destRect.left + destRect.width / 2 - 48,
            y: destRect.top + destRect.height / 2 - 72,
            scale: 0.5, // Small size for board
            duration: 0.6,
            ease: 'power2.inOut'
        }, 'atCenter+=0.5') // Wait 0.5s at center
        
        timeline.to(floatingCard, {
            opacity: 0, // Fade out once arrived
            duration: 0.1
        })
    } else {
        // For brown cards, move to discard pile
        const discardEl = document.querySelector('[data-discard-pile]')
        
        if (discardEl) {
            const discardRect = discardEl.getBoundingClientRect()
            timeline.to(floatingCard, {
                x: discardRect.left + discardRect.width / 2 - 48,
                y: discardRect.top + discardRect.height / 2 - 72,
                scale: 1,
                rotation: (Math.random() - 0.5) * 30,
                duration: 0.5,
                ease: 'power2.inOut'
            }, 'atCenter+=0.5')
        } else {
            // Fallback if discard pile not found
            timeline.to(floatingCard, {
                opacity: 0,
                scale: 0.8,
                duration: 0.3
            }, 'atCenter+=0.5')
        }
    }
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

  function animateCardCheck(event, resolve) {
    const deckEl = document.querySelector('[data-deck-pile]')
    const discardEl = document.querySelector('[data-discard-pile]')

    if (!deckEl || !discardEl) {
      resolve()
      return
    }

    const deckRect = deckEl.getBoundingClientRect()
    const discardRect = discardEl.getBoundingClientRect()
    
    const viewportCenterX = window.innerWidth / 2 - 48
    const viewportCenterY = window.innerHeight / 2 - 72

    // 0. Focus (Dim Background)
    const overlay = document.createElement('div')
    overlay.className = 'fixed inset-0 bg-black/60 z-[150] pointer-events-none opacity-0'
    document.body.appendChild(overlay)

    // 1. Create Card Container with Inner Structure for Proper 3D Flip
    // IMPORTANT: Inline styles are safer to avoid Tailwind purge/conflict issues with dynamic elements
    const cardContainer = document.createElement('div')
    cardContainer.style.cssText = `
        position: fixed;
        width: 96px;
        height: 144px;
        z-index: 200;
        perspective: 1000px;
        pointer-events: none;
        top: 0;
        left: 0;
    `
    
    const cardInner = document.createElement('div')
    cardInner.style.cssText = `
        position: relative;
        width: 100%;
        height: 100%;
        transform-style: preserve-3d;
    `
    
    // Front Face (The actual card value, initially rotated 180deg to be hidden)
    const cardFront = document.createElement('div')
    cardFront.style.cssText = `
        position: absolute;
        width: 100%;
        height: 100%;
        backface-visibility: hidden;
        -webkit-backface-visibility: hidden;
        transform: rotateY(180deg);
        border-radius: 0.5rem;
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
        overflow: hidden;
    `
    // Re-use card-game style manually to ensure visibility
    cardFront.classList.add('card-game')
    
    const frontImg = document.createElement('img')
    frontImg.src = getCardImage(event.cardType)
    frontImg.style.cssText = 'width: 100%; height: 100%; object-fit: cover;'
    cardFront.appendChild(frontImg)

    // Retrieve suit/value data from event (must be added to event payload first)
    if (event.data && event.data.suit && event.data.value) {
        const badge = document.createElement('div')
        badge.style.cssText = `
            position: absolute;
            bottom: -2px;
            left: 3px;
            display: flex;
            align-items: flex-end;
            gap: 1px;
            z-index: 10;
            padding: 0;
            background: transparent;
        `
        
        const valueSpan = document.createElement('span')
        valueSpan.textContent = event.data.value
        // text-xs font-semibold (approx 12px) - matching standard cards
        valueSpan.style.cssText = 'font-size: 0.75rem; font-weight: 600; color: #1f2937; text-shadow: 0 0 2px white;'
        
        const suitSpan = document.createElement('span')
        suitSpan.textContent = getSuitSymbol(event.data.suit)
        const isRed = event.data.suit === 'HEARTS' || event.data.suit === 'DIAMONDS'
        // text-sm font-bold (approx 14px) - matching standard cards
        suitSpan.style.cssText = `font-size: 0.875rem; font-weight: 700; color: ${isRed ? '#ef4444' : '#1f2937'}; text-shadow: 0 0 2px white;`
        
        badge.appendChild(valueSpan)
        badge.appendChild(suitSpan)
        cardFront.appendChild(badge)
    }
    
    // Back Face (The deck back, initially visible at 0deg)
    const cardBack = document.createElement('div')
    cardBack.style.cssText = `
        position: absolute;
        width: 100%;
        height: 100%;
        backface-visibility: hidden;
        -webkit-backface-visibility: hidden;
        border-radius: 0.5rem;
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
        overflow: hidden;
    `
    // Apply card-back style manually if class fails
    cardBack.classList.add('card-game', 'card-back')
    
    const backImg = document.createElement('img')
    backImg.src = '/images/common/deck.png'
    backImg.style.cssText = 'width: 100%; height: 100%; object-fit: cover;'
    cardBack.appendChild(backImg)
    
    cardInner.appendChild(cardFront)
    cardInner.appendChild(cardBack)
    cardContainer.appendChild(cardInner)
    document.body.appendChild(cardContainer)
    
    // Initial Position (At Deck)
    gsap.set(cardContainer, {
        x: deckRect.left,
        y: deckRect.top,
        scale: 1,
        zIndex: 200
    })

    const timeline = gsap.timeline({
        onComplete: () => {
            cardContainer.remove()
            overlay.remove()
            resolve()
        }
    })

    // Phase 1: Lift & Move to Center (Face Down)
    timeline.to(overlay, { opacity: 1, duration: 0.5 }, 0)
    
    timeline.to(cardContainer, {
        x: viewportCenterX,
        y: viewportCenterY,
        scale: 2.5,
        duration: 0.8,
        ease: 'power2.out'
    }, 0)

    // Phase 2: 3D Flip (Reveal)
    // Rotate the inner container 180 degrees to show front
    timeline.to(cardInner, {
        rotationY: 180,
        duration: 0.6,
        ease: 'back.out(1.2)'
    })

    // Phase 3: Showcase (Pause)
    timeline.to(cardContainer, {
        scale: 2.6, // Slight zoom in
        duration: 1.5,
        yoyo: true,
        repeat: 1,
        ease: 'sine.inOut'
    })

    // Phase 4: Discard (Shrink & Move to Discard Pile)
    timeline.to(overlay, { opacity: 0, duration: 0.5 }, ">-0.5")
    
    timeline.to(cardContainer, {
        x: discardRect.left + discardRect.width / 2 - 48,
        y: discardRect.top + discardRect.height / 2 - 72,
        scale: 1,
        rotation: 360, // Spin while moving
        duration: 0.6,
        ease: 'power2.in'
    }, "<")
  }

  return {
    queueAnimation,
    processQueue,
    queue,
    isProcessing,
    animatingCardIds
  }
}
