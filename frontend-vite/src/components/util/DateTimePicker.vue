<script setup lang="ts">

import  {DateTime} from "luxon";
import {computed, onMounted, type PropType, ref} from "vue";
import {templateRef} from "@vueuse/core";
import {firstDateFromCalendarValue, type CalendarModelValue} from "@/utils/calendar";


type ModelValueType = DateTime | undefined;

const model = defineModel<ModelValueType>({
  // For Vue's runtime check, use Object
  type: Object as PropType<ModelValueType>,
  required: false,
  // Optional: Add a default if necessary
  // default: undefined, // Or () => null if using null
});

const props = defineProps({
  correctStartDate: {
    type: Object as PropType<ModelValueType>,
    required: false
  },
  timeOnly: {
    type: Boolean,
    required: false,
    default: false
  },

})

function onCalendarChange(value: CalendarModelValue) {
  const date = firstDateFromCalendarValue(value)
  if (!date) {
    return
  }

  model.value = correctStartTime(DateTime.fromJSDate(date))
}

function onTimeInputChange(event: Event) {
  const input = event.target as HTMLInputElement;
  if (input.value) {
    const dateFromInput = DateTime.fromISO(input.value)
    if(dateFromInput==null) {
      console.error("Date from input is null")
      return
    }
    const calculatedDateTime = correctStartTime(dateFromInput);
    timeInput.value.focus()
    console.log("CalculatedDateTime:",calculatedDateTime)
    model.value = calculatedDateTime
  }
}

function correctStartTime(value:DateTime, useTimeZone = true): DateTime {
  if (props.correctStartDate) {
    //Calendar has the issue that it may change the date to the actual date if I enter the time by hand
    //so we reset the date to the tournament date, but keep the time
    const tournamentDate = props.correctStartDate
    if (value) {
      return DateTime.fromObject({
        year: tournamentDate.year,
        month: tournamentDate.month,
        day: tournamentDate.day,
        hour: value.hour,
        minute: value.minute,
        second: value.second
      }, {zone: useTimeZone ? value.zone : undefined})
    }
  }
  return value
}
const isMobile = ref(false)

onMounted(() => {
  isMobile.value = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  const nativePickerAvailable = 'showPicker' in HTMLInputElement.prototype
  isMobile.value = isMobile.value && nativePickerAvailable
})

const timeInput = templateRef<HTMLInputElement>("timeInput")

function handleCalendarClick(event: Event) {
  console.log("handleCalendarClick", event)
  if(isMobile.value && timeInput.value) {
    console.log("handleCalendarClick")
    timeInput.value.focus()
    timeInput.value.showPicker()

    event.stopPropagation()
  }

}

const mobileInputType = computed(() => {
  return props.timeOnly ? "time" : "datetime-local"
})

const nativeInputValue = computed(() => {
  if (model.value) {
    return model.value.toISODate()+"T"+model.value.toLocaleString(DateTime.TIME_24_SIMPLE)
  }
  return ""
})
</script>

<template>
  <div class="relative">
  <DatePicker
      v-bind="$attrs"
      :model-value="model?.toJSDate()"
      @update:model-value="onCalendarChange"
      id="timeStartGame"
      :showSeconds="false"
      :showTime="true"
      :time-only="timeOnly"
      date-format="yy-mm-dd"
      :readonly="isMobile"
      :pt="{
        'pcInputText': {
          root: 'z-20 w-48'
        }
      }"

      @click="handleCalendarClick"

  />
  <input
      v-if="isMobile"
      ref="timeInput"
      :type="mobileInputType"
      id="timeStartGameNative"
      class="z-[-1] text-xs absolute left-0 h-1"
      :value="nativeInputValue"
      @input="onTimeInputChange"
  />
  </div>
</template>

<style scoped>

</style>
