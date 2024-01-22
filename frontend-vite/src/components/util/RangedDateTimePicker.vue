<script setup lang="ts">

import {DateTime} from "luxon";
import {computed} from "vue";

const props = defineProps<{
  dateRange: Date[]
}>()

const emit = defineEmits<{
  (e: 'update:dateRange', value: Date[]): void
}>()
function setToAllTime() {
  localDateRange.value = [
    DateTime.fromMillis(0).toJSDate(),
    DateTime.now().toJSDate()
  ]
}


const localDateRange = computed({
  get() {
    return props.dateRange
  },
  set(newValue) {
    emit('update:dateRange', newValue)
  }
})

</script>

<template>
  <div class = "flex flex-row justify-center">
    <Calendar v-model="localDateRange" selectionMode="range" :manualInput="false" />
    <Button text @click="localDateRange = [DateTime.now().minus({days: 30}).toJSDate(), new Date()]">Last 30 days</Button>
    <Button text @click="localDateRange = [DateTime.now().minus({days: 180}).toJSDate(), new Date()]">Last 180 days</Button>
    <Button text @click="localDateRange = [DateTime.now().minus({year: 1}).toJSDate(), new Date()]">Last 365 days</Button>
    <Button text @click="localDateRange = [DateTime.now().minus({year: 1}).startOf('year').toJSDate(), DateTime.now().minus({year: 1}).endOf('year').toJSDate()]">Last year</Button>
    <Button text @click="setToAllTime">All time</Button>
  </div>
</template>

<style scoped>

</style>
