<script setup lang="ts">

import {computed, onMounted, ref, watch} from "vue";
import {useFocus, watchIgnorable} from "@vueuse/core";

const points = defineModel<number>('points')
const goals = defineModel<number>('goals')

const inputValue = ref('')

const matchPattern = /^(\d+)?\s*(?:-|\s*|,|.)\s*(\d+)?$/

/**
 * We cant use watchIgnorable with defineModel as writing to the model
 * will only trigger the emit call. Then the model is updated from outside.
 * Until then, the watchIgnorable is no longer active, so we cannot ignore a status change.
 */
const ignoreNextValueUpdate = ref(false)
function validateInput(event: Event) {
  const target = event.target as HTMLInputElement
  const lastEntered = target.value
  if (matchPattern.test(lastEntered)) {
    ignoreNextValueUpdate.value = true
    const match = matchPattern.exec(lastEntered)


    goals.value = parseInt(match?.[1] || "0")
    points.value = parseInt(match?.[2] || "0")

    inputValue.value = lastEntered
  } else {
    target.value = inputValue.value

  }

}

onMounted(() => {
  formatInput()
})


function formatInput() {
  inputValue.value = `${goals.value} - ${points.value}`
}

const inputTarget = ref()
const {focused} = useFocus(inputTarget)

watch(focused, (value) => {
  if (!value) {
    formatInput()
  }
})

watch(
    [points, goals],
    () => {
      if (ignoreNextValueUpdate.value) {
        ignoreNextValueUpdate.value = false
        return
      }
      formatInput()
      //inputValue.value = `${goals.value} - ${points.value}`
    }, {
      immediate: true,
    }
)

declare global {
  interface Window {
    MSStream: any;
  }
}
const isIOS = computed(() => /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream)
</script>

<template>
  <div>
  Score:
    <input
        v-if="!isIOS"
        type="text"
        inputmode="numeric"
        pattern="[0-9]*"
        :value="inputValue"
        @input="validateInput"
        ref="inputTarget"
        class="p-2 w-16 rounded border border-gray-600"
    />
    <div v-else class="flex flex-row justify-start align-center rounded border border-gray-600 bg-white">
      <!-- Awful hack for iOS as it does not have a keyboard with a minus sign -->
      <input
        type="number"
        inputmode="numeric"
        pattern="[0-9]*"
        v-model="goals"
        class="p-2 w-10 text-end"
        @focus="($event.target as HTMLInputElement).select()"
        />
      <div class="pt-2">-</div>
       <input
        type="number"
        inputmode="numeric"
        pattern="[0-9]*"
        v-model="points"
        class="p-2 w-10 ml-2"
        @focus="($event.target as HTMLInputElement).select()"
       />
    </div>
  </div>
</template>

<style scoped>

</style>
