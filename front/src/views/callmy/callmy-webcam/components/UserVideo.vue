<template>
  <div v-if="streamManager && isPlayer && state.nowPlayUserNames.includes(state.clientData)" style="position:relative">
    <ov-video :username="state.clientData" :startDetection="startDetection" :stream-manager="streamManager"/>
    <div class="nameTag-bottom"><p>{{ state.clientData }}</p></div>
  </div>

  <div v-else-if="streamManager && !isPlayer && !state.nowPlayUserNames.includes(state.clientData) " style="position:relative">
    <ov-video :username="state.clientData" :startDetection="startDetection" :stream-manager="streamManager"/>
    <div class="nameTag-top"><p>{{ state.clientData }}</p></div>
  </div>

</template>
<style scoped>
  @import url('./uservideo.css');
</style>
<script>
import OvVideo from './OvVideo.vue'
import { computed, reactive } from 'vue'
import { useStore } from 'vuex'

export default {
	name: 'UserVideo',
	components: {
		OvVideo,
	},
	props: {
		streamManager: Object,
    startDetection: Boolean,
    isPlayer: Boolean,
	},
  setup(props, { emit }) {
    const store = useStore()

    const state = reactive({
			clientData: computed(() => {
        const { clientData } = getConnectionData()
        return clientData
      }),
      nowPlayUserNames: computed(() => {
        if (store.state.root.callmyManager.nowPlayUsers) {
          return store.state.root.callmyManager.nowPlayUsers.map(playUserObj => {
            return playUserObj.username
          })
        }
        return []
      })
    })

    const getConnectionData = () => {
			const { connection } = props.streamManager.stream;
			return JSON.parse(connection.data)
		}


    return { state }
  },

}
</script>

