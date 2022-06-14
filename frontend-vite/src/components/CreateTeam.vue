<script setup lang="ts">
import {onMounted, ref} from "vue";
import type Team from "@/types";

const props = defineProps<{
  rough_team_name?:string
}>()

const emit = defineEmits<{
  (e: 'on_new_team', team: Team): void
}>()

onMounted(()=> {
  console.log("Mounted CreateTeam")
  new_team_name.value = props.rough_team_name || ""
})
const new_team_name = ref("")
const is_loading = ref(false)
async function send_new_team_to_server() {
  if (new_team_name) {
    const requestOptions = {
      method: "POST",
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      },
      body: JSON.stringify({ "name": new_team_name.value})
    };
    is_loading.value = true
    const response = await fetch("/api/new_team",requestOptions)
    const data = await response.json()
    is_loading.value = false
    emit('on_new_team',data as Team)
  }
}
</script>

<template>
  <label for="new_team_name">Name of new team:</label><br>
  <input id="new_team_name" :disabled="is_loading" v-model="new_team_name"><br>
  <button @click="send_new_team_to_server">Send</button>
</template>



<style scoped>

</style>