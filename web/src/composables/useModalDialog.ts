import { nextTick, onBeforeUnmount, ref, watch, type Ref } from 'vue'

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

/** Adds focus containment, Escape closing, and focus restoration to an existing modal. */
export function useModalDialog(isOpen: Ref<boolean>, close: () => void, getFallbackFocus?: () => HTMLElement | null) {
  const dialogRef = ref<HTMLElement | null>(null)
  let activeDialog: HTMLElement | null = null
  let returnFocusTarget: HTMLElement | null = null

  function getFocusableElements(root: HTMLElement): HTMLElement[] {
    return Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
      element => !element.hasAttribute('hidden') && element.getAttribute('aria-hidden') !== 'true',
    )
  }

  function handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      event.preventDefault()
      event.stopPropagation()
      close()
      return
    }

    if (event.key !== 'Tab' || !activeDialog) return

    const focusable = getFocusableElements(activeDialog)
    if (focusable.length === 0) {
      event.preventDefault()
      activeDialog.focus()
      return
    }

    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    const activeElement = document.activeElement

    if (!activeDialog.contains(activeElement)) {
      event.preventDefault()
      first.focus()
    } else if (event.shiftKey && (activeElement === first || activeElement === activeDialog)) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && (activeElement === last || activeElement === activeDialog)) {
      event.preventDefault()
      first.focus()
    }
  }

  function detachDialog() {
    activeDialog?.removeEventListener('keydown', handleKeydown)
    activeDialog = null
  }

  watch(isOpen, async open => {
    if (!open) {
      detachDialog()
      const restoreTarget = returnFocusTarget?.isConnected ? returnFocusTarget : getFallbackFocus?.()
      if (restoreTarget?.isConnected) restoreTarget.focus()
      returnFocusTarget = null
      return
    }

    returnFocusTarget = document.activeElement instanceof HTMLElement ? document.activeElement : null
    await nextTick()
    activeDialog = dialogRef.value
    if (!activeDialog) return

    activeDialog.addEventListener('keydown', handleKeydown)
    const firstFocusable = getFocusableElements(activeDialog)[0]
    ;(firstFocusable || activeDialog).focus()
  }, { flush: 'post' })

  onBeforeUnmount(detachDialog)

  return { dialogRef }
}
