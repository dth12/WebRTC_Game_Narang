<template>
  <div class="setting-container">
    <div class="game-btns" v-show="state.disableGameBtns">
      <div class="game-start" @click="gameStart" style="border-top-right-radius: 0px; border-bottom-right-radius: 0px;">Game start!</div>
      <div class="game-select" style="border-top-left-radius: 0px; border-bottom-left-radius: 0px;">
        <img
          :src="require('@/assets/images/game-thumbnail-mafia.png')"
          @click="updateGameInfo"
          data-game="mafia"
          class="game-img">
        <img
          :src="require('@/assets/images/game-thumbnail-callmy.png')"
          @click="updateGameInfo"
          data-game="callmy"
          class="game-img">
        <img
          :src="require('@/assets/images/comming-soon.png')"
          data-game="callmy"
          class="game-img">
        <img
          :src="require('@/assets/images/comming-soon.png')"
          data-game="callmy"
          class="game-img">
        <img
          :src="require('@/assets/images/comming-soon.png')"
          data-game="callmy"
          class="game-img">
      </div>
    </div>

    <div class="setting-btns">
      <div v-if="state.onAudio" class="setting-btn" @click="muteAudio"> <i class="el-icon-microphone"></i></div>
      <div v-if="!state.onAudio" class="setting-btn" @click="muteAudio"><i class="el-icon-turn-off-microphone"></i></div>

      <div v-if="state.onVideo" class="setting-btn" @click="muteVideo"><i class="el-icon-video-camera"></i></div>
      <div v-if="!state.onVideo" class="setting-btn" @click="muteVideo"><i class="el-icon-video-pause"></i></div>

      <div class="setting-btn" @click="openDialog"><i class="el-icon-setting"></i></div>
      <div class="setting-btn" @click="leaveRoom"><i class="el-icon-close"></i></div>
    </div>
  </div>
</template>
<style scoped>
  @import url('./game-room-setting.css');
</style>
<script>
import { ElMessage } from 'element-plus'
import { useStore } from 'vuex'
import { reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'

export default {
  name: 'GameRoomSetting',
  props: {
    roomId: {
      type: Number
    },
    room: {
      type: Object
    }
  },

  setup(props, { emit }) {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    const state =  reactive({
      onVideo : true,
      onAudio : true,
      disableGameBtns : undefined,
      ownerId : undefined,
      userId : undefined,
      disableGameBtns: true,
    })

    const openDialog = () => {
      emit('openDialog')
    }

    const updateGameInfo = (event) => {
      const game = event.target.dataset.game
      const roomInfo = {
        ...props.room,
        game,
      }
      console.log(roomInfo)
      store.dispatch('root/requestUpdateGameRoom', roomInfo)
        .then(res => {
          emit('changeGame')
        })
        .catch(err => {
          ElMessage({
            type: 'error',
            message: '????????? ????????? ??? ????????????.'
          })
        })
    }

    const gameStart = () => {
      emit('gameStart')
    }

    const leaveRoom = () => {
      router.push({
        name: 'waitingRoom'
      })
    }

    const muteAudio = () => {
        state.onAudio = !state.onAudio;
        console.log(state.onAudio)
        store.state.root.publisher.publishAudio(state.onAudio);


    }
    const muteVideo = () => {
        state.onVideo = !state.onVideo;
        console.log(state.onVideo)
        store.state.root.publisher.publishVideo(state.onVideo);
    }

    const setBtnDisabled = () => {
      store.dispatch('root/requestReadSingleGameRoom', props.roomId).then(roomRes => {
          store.dispatch('root/requestReadMyInfo').then(userRes => {
              state.userId = userRes.data.user.userId; // ?????? ?????? ID
              state.ownerId = roomRes.data.room.ownerId; // ??? ?????? ID
              if(state.ownerId == state.userId) state.disableGameBtns = true; // ????????? ?????? ?????? ?????????
              else state.disableGameBtns = false; // ????????? ?????? ?????? ??? ?????????
          })
          .catch(err => {
            ElMessage(err)
          })
      })
      .catch(err => {
        ElMessage(err)
      })
    }

    //* created *//
    setBtnDisabled()

    return {state ,route, openDialog, updateGameInfo, leaveRoom, muteAudio, muteVideo, gameStart}
  }
}
</script>


