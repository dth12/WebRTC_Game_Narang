<template>
  <div class="board-container">
    <div class="board-title">Notice 📣</div>
    <hr>
    <Notice :msg="msg" :voteStatus="voteStatus"/>
    <hr>
    <Vote v-if="state.mafiaManager.isAlive === true && isVoteTime === true"/>
  </div>
</template>

<script>
import Notice from './notice.vue'
import Vote from './vote.vue'

import { computed, reactive } from 'vue'
import { useStore } from 'vuex'

export default {
  name: 'board',

  props: {
    msg: {
      type: String,
    },
    isVoteTime: {
      type: Boolean,
    },
    voteStatus: {
      type: Object
    }
  },

  components: {
    Notice,
    Vote,
  },

  setup(props, { emit }) {
    const store = useStore()

    const state = reactive({
      mafiaManager: computed(() => store.getters['root/mafiaManager']),
    })

    return { state }
  }
}
</script>
<style scoped>
  .board-container {
    padding: 10px;
  }

  .board-title {
    text-align: center;
    font-size: 20px;
    font-weight: 700;
    margin-bottom: 10px;
  }
</style>
