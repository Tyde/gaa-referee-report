<script setup lang="ts">
import {onMounted, ref} from "vue";
import {createTeam} from "@/utils/api/teams_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";
const store = useReportStore()
const props = defineProps<{
  rough_team_name?:string
}>()

const emit = defineEmits<{
  (e: 'on_new_team', team: Team): void,
  (e: 'on_cancel'): void
}>()

onMounted(()=> {
  console.log("Mounted CreateTeam")
  new_team_name.value = props.rough_team_name || ""
})
const new_team_name = ref("")
const is_loading = ref(false)
async function send_new_team_to_server() {
  if (new_team_name.value != "") {

    is_loading.value = true
    let data = await createTeam(new_team_name.value)
        .catch((error) => {
          store.newError(error)
          return undefined
        })
    is_loading.value = false
    emit('on_new_team',data as Team)
  }
}
</script>

<template>
  <label for="new_team_name">Name of new team:</label><br>
  <InputText id="new_team_name" :disabled="is_loading" v-model="new_team_name" /><br>
  <div class="flex flex-row">
    <div class="m-2">
      <Button @click="send_new_team_to_server">Send</Button>
    </div>
    <div class="m-2">
      <Button @click="emit('on_cancel')">Cancel</Button>
    </div>
  </div>
</template>



<style scoped>

</style>