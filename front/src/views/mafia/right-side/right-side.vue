<template>
  <div class="right-side-container">
    <div class="right-btn-group">
      <el-tooltip class="item" effect="light" content="롤카드 보기" placement="top">
        <img @click="clickGetRole" :src="require('@/assets/images/icon/role.png')" alt="롤카드 보기">
      </el-tooltip>
      <el-tooltip class="item" effect="light" content="거짓말 탐지" placement="top">
        <img @click="clickLie" :src="require('@/assets/images/icon/liar.png')" alt="거짓말 탐지">
      </el-tooltip>
      <el-tooltip class="item" effect="light" content="미션 보기" placement="top">
        <img @click="clickShowMission" :src="require('@/assets/images/icon/mission.png')" alt="미션 보기">
      </el-tooltip>
    </div>
    <div v-if="state.myRole === 'Mafia'" id="mission-container">
      <div id="mission-progress"></div>
      <div id="mission-message"></div>
    </div>
    <Timer class="right-timer" :timer="timer"/>
    <Board class="right-board" :msg="msg" :isVoteTime="isVoteTime" :voteStatus="voteStatus"/>
    <Setting class="right-setting" @clickGuide="clickGuide"/>
  </div>
</template>

<script>
import Board from './board/board.vue'
import Timer from './timer/timer.vue'
import Setting from './setting/setting.vue'

import { reactive, computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'

export default {
  name: 'rightSide',

  props: {
    msg: {
      type: String,
    },
    isVoteTime: {
      type: Boolean,
    },
    timer: {
      tpye: Number,
    },
    voteStatus: {
      type: Object,
    }
  },

  components: {
    Board,
    Timer,
    Setting,
  },

  setup(props, { emit }) {
    const store = useStore()
    const route = useRoute()
    const router = useRouter()

    const state = reactive({
      myRole: computed(() => store.state.root.mafiaManager.myRole)
    })

    const clickGetRole = () => {
      emit('sendGetRole')
    }

    const clickShowMission = () => {
      emit('clickShowMission')
    }

    const clickLie = () => {
      emit('clickLie')
    }

    const clickGuide = () => {
      emit('clickGuide')
    }

    return { state, clickGetRole, clickShowMission, clickLie, clickGuide }
  }
}
</script>

<style scoped>
  @import url('./right-side.css');
</style>
