<template>
  <div class="callmy-container">
    <div class="callmy-left-side">
      <CallMyWebCam
        @joinSomeOne="joinSomeOne"
        @joinCallMyRoom="joinCallMyRoom"
        :socketConnected="state.socketConnected"
        :roomId="route.params.roomId"
        :gameStart="state.isAllConnected"
        :roundStart="state.roundStart"
        :playerNumbers="state.userList.length"
        :startDetection="state.startDetection"
        :speaker="state.speaker"
        :answer="state.answer"/>
    </div>
    <div class="callmy-right-side">
      <CallMyGameBoard
        :nicknameList="state.nicknameList"
        :order="state.order"
        :isVoteTime="state.isVoteTime"
        :defaultNickname="store.state.root.callmyManager.defaultNickname"
        @sendVote="sendVote"/>
      <CallMyChat
        :chatList="state.chatList"
        :roomId="route.params.roomId"
        @sendChat="sendChat"/>
      <CallmySetting @clickGuide="state.callmyGuideVisible = true"/>
    </div>
  </div>
  <CallmyStt
    @sendGuessName="sendGuessName"
    :speaker="state.speaker"
    :yesOrNo="state.yesOrNo"
    v-if="!state.isVoteTime && state.callmyManager.nowPlayUsers.length && (state.userId === state.callmyManager.nowPlayUsers[0].userId || state.userId === state.callmyManager.nowPlayUsers[1].userId)"/>
  <Dialog v-if="state.callmyGuideVisible" @click="state.callmyGuideVisible = false">
    <CallmyGuide/>
  </Dialog>

  <Dialog v-if="state.isNoticeVisible">
    <CallmyNotice :msg="state.msg" :msgType="state.msgType"/>
  </Dialog>

  <div class="yesOrNo-dialog" v-if="state.yesOrNo">
    <div :class="{'stt-yesOrNo':true, 'stt-yes': state.yesOrNo === 'O', 'stt-no': state.yesOrNo === 'X'}">{{ state.yesOrNo }}</div>
  </div>

  <CallmyBackground/>
</template>
<style scoped>
  @import url('./callmy.css');
  @import url('./callmy-stt/callmy-stt.css');
</style>
<script>
import Stomp from 'webstomp-client'
import SockJS from 'sockjs-client'

import CallMyWebCam from './callmy-webcam/callmy-webcam.vue'
import CallMyChat from './callmy-chat/callmy-chat.vue'
import CallMyGameBoard from './callmy-gameboard/callmy-gameboard.vue'
import CallmyBackground from './callmy-background/callmy-background.vue'
import CallmySetting from './callmy-setting/callmy-setting.vue'
import CallmyStt from './callmy-stt/callmy-stt.vue'
import Dialog from '@/views/common/dialog/dialog.vue'
import CallmyGuide from './callmy-guide/callmy-guide.vue'
import CallmyNotice from './callmy-notice/callmy-notice.vue'

import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { reactive, computed, onBeforeUnmount } from 'vue'

export default {
  name: 'callMy',
  components: {
    CallMyWebCam,
    CallMyChat,
    CallMyGameBoard,
    CallmyBackground,
    CallmySetting,
    CallmyStt,
    Dialog,
    CallmyGuide,
    CallmyNotice,
  },

  setup(props, { emit }) {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()


    const state = reactive({
      room: computed(() => store.state.root.myRoom),
      stompClient: null,
      chatList: [],
      userList: {},
      callmyManager: computed(() => store.state.root.callmyManager),
      userId: computed(() => store.state.root.userId),
      username: computed(() => store.state.root.username),
      socketConnected: false,
      nicknameList: {},
      userIdToUserName: {},
      showResult: false,
      order: 0,
      isVoteTime: false,
      startDetection: false,
      isAllConnected: false,
      roundStart: false,
      round: 1,
      answer: '',
      speaker: '',
      callmyGuideVisible: false,
      isNoticeVisible: false,
      msg: '',
      timeout: 5000,
      msgType: 'default',
      yesOrNo: '',
    })


    const connectSocket = () => {
      let socket = new SockJS("/narang")
      state.stompClient = Stomp.over(socket)
      state.stompClient.connect({}, () => {
          state.socketConnected = true // webcam?????? connected ????????? props??? ??????.
          subscribeChat() // ?????? ??????
          subscribeCheckConnect() // ?????? ????????? ?????????????????? ?????? true or false ?????? ??????
          subscribeGuessName() // ???????????? ????????? ????????? ?????? ??? ???????????? ?????????
          subscribeSetName() // ??????????????? ???????????? ????????? ??? ???????????? ?????????
          subscribePlay()
          subscribeDefaultNickname()
        }
      )
    }


    const subscribeChat = () => {
      state.stompClient.subscribe(`/from/call/chat/${route.params.roomId}`, res => {
        const chat = JSON.parse(res.body)
        state.chatList.push(chat)
      })
    }


    const subscribeCheckConnect = () => {
      state.stompClient.subscribe(`/from/call/checkConnect/${route.params.roomId}`, res => {
        state.isAllConnected = true
        state.draw = JSON.parse(res.body)
        state.isVoteTime = true
        state.msg = '?????? ??? ????????? ???????????????.'
        state.isNoticeVisible = true
        setTimeout(() => {
          state.msg = ''
          state.isNoticeVisible = false
          sendPlay('next')
        }, state.timeout);
      })
    }


    const subscribeGuessName = () => {
      state.stompClient.subscribe(`/from/call/guess-name/${route.params.roomId}`, res => {
        const guessNameRes = JSON.parse(res.body)
        console.log("guessNameRes : ", guessNameRes)
        console.log('??? ?????? : ', state.username)
        console.log('?????? ????????? ?????? ?????? : ' , state.userIdToUserName[guessNameRes.userId])
        console.log('?????? ????????? ?????? ????????? ?????? ?????? : ' , guessNameRes.answer)
        if(guessNameRes.answer === '???????????????'){
          endAnswerTime();
          return;
        }
        if(guessNameRes.answer === '??????'){
          console.log("?????? ????????? ????????? ??????????????")
          console.log(store.state.root.callmyManager.isAnswer)
          if(store.state.root.callmyManager.isAnswer) return;
          store.state.root.callmyManager.isAnswer = true;
          console.log("????????? ????????? ??? ????????? ????????????~")
          console.log(state.callmyManager.isAnswer)
        }

        state.speaker = state.userIdToUserName[guessNameRes.userId];
        if(guessNameRes.answer !== '??????'){
          state.answer = guessNameRes.answer;
        }
        if(guessNameRes.gameEnd) {
          const audio = new Audio();
          audio.src = "@/assets/audio/callmy/correct.mp3"
          audio.play();
          state.yesOrNo = 'O'
          setTimeout(() => {
            state.yesOrNo = ''
            state.msg = `?????? ???????????? ${state.speaker}??? ?????????! ????????? ????????? ???????????????.`
            state.isNoticeVisible = true
            state.msgType = 'win'
            setTimeout(() => {
              gameOver();
            }, state.timeout);
          }, 1000)
          return;
        }

        console.log('?????? ?????? ?????? ???' ,state.yesOrNo)
        if(guessNameRes.correct) {
          const audio = new Audio(require('@/assets/audio/callmy/correct.mp3'));
          audio.play();
          console.log(`${state.speaker}??? ??????????????????`)
          state.yesOrNo = 'O'
          console.log('?????? ????????? ???' ,state.yesOrNo)
          setTimeout(() => {
            state.yesOrNo = ''
            state.isVoteTime = true
            state.roundStart = false
            state.startDetection = false
            state.msg = `${state.speaker}?????? ??????????????????. ????????? ?????? ???????????? ???????????????!`
            state.isNoticeVisible = true
            state.msgType = 'win'
            setTimeout(() => {
              state.msg = ''
              state.isNoticeVisible = false
              state.msgType = 'default'
              endAnswerTime();
              sendPlay('next')
            }, state.timeout);
          }, 1000)
          return;
        }

        if(guessNameRes.answer !== '??????') {
          state.yesOrNo = 'X'
          console.log('?????? ????????? ???' ,state.yesOrNo)
          setTimeout(() => {
            state.yesOrNo = ''
          }, 1000)
          console.log("???????????????.")
        }
      })
    }


    const subscribePlay = () => { // ?????? ?????? ?????? ???????????? ??????, ?????? ?????????, status => 0: ????????? ??????, 1: ?????? ???,
      state.stompClient.subscribe(`/from/call/play/${route.params.roomId}`, res => {
        const result = JSON.parse(res.body)
        console.log(result, '?????? ????????????')
        store.state.root.callmyManager.round = result.round;
        if(result.status === 0){ // ????????? ????????? ??????
          store.state.root.callmyManager.nowPlayUsers = [
            {
              userId: result.user1.userId,
              username: state.userIdToUserName[result.user1.userId],
              nickname: '',
            },
            {
              userId: result.user2.userId,
              username: state.userIdToUserName[result.user2.userId],
              nickname: '',
            }
          ];
          state.isVoteTime = true
          sendDefaultNickname() // 1??? ?????? ????????? ????????? ??????
          showResult()
        } else { // ????????? ?????? ??????
          store.state.root.callmyManager.nowPlayUsers[0].nickname = result.user1.nickname
          store.state.root.callmyManager.nowPlayUsers[1].nickname = result.user2.nickname
          state.startDetection = true
        }
      })
      console.log(state.callmyManager)
    }


    const subscribeSetName = () => {
      state.stompClient.subscribe(`/from/call/set-name/${route.params.roomId}`, res => {
        const setNamRes = JSON.parse(res.body)
        if (setNamRes.isFinished) {
          if (state.callmyManager.nowPlayUsers[0].nickname){ // user1??? ???????????? ????????? user2 ????????? ??????
            state.callmyManager.nowPlayUsers[1].nickname = setNamRes.result
            console.log(`${state.callmyManager.nowPlayUsers[1].username}??? ???????????? ${setNamRes.result}?????????`)
            state.nicknameList = {}
            state.order = 0
            state.msg = `????????? ????????? ?????????????????????. ????????? ?????? ??????!`
            state.isNoticeVisible = true
            setTimeout(() => {
              state.msg = ''
              state.isNoticeVisible = false
              sendPlay('now')
            }, state.timeout);
            state.isVoteTime = false
          } else { // user1??? ???????????? ????????? user1 ????????? ??????
            state.callmyManager.nowPlayUsers[0].nickname = setNamRes.result
            console.log(`${state.callmyManager.nowPlayUsers[0].username}??? ???????????? ${setNamRes.result}?????????`)
            state.nicknameList = {}
            state.order = 1
            sendDefaultNickname() // ????????? ?????? ????????? ?????? ????????????
          }
        } else {
          state.nicknameList = setNamRes.voteStatus
        }
        console.log("setNamRes")
        console.log(setNamRes)
      })
    }


    const subscribeDefaultNickname = () => {
      state.stompClient.subscribe(`/from/call/default-name/${route.params.roomId}/${state.userId}`, res => {
        console.log(res)
        console.log(res.body)
        const DefaultNickname = res.body
        if (state.userId !== state.callmyManager.nowPlayUsers[state.order].userId) {
          store.state.root.callmyManager.defaultNickname = DefaultNickname
        } else {
          store.state.root.callmyManager.defaultNickname = ''
        }
      })
    }


    const sendChat = (message) => {
      if (state.stompClient && state.stompClient.connected) {
        state.stompClient.send(`/to/call/chat/${route.params.roomId}`, JSON.stringify(message), {})
      }
    }


    const sendVote = (message) => {
      if (state.stompClient && state.stompClient.connected) {
        state.stompClient.send(`/to/call/set-name/${route.params.roomId}`, JSON.stringify(message), {})
      }
    }


    const sendPlay = (stage) => {
      if (stage === 'next') {
        console.log('????????? roundStart, startDetection ?????????')
        state.roundStart = true
        state.startDetection = false
      }
      if (state.stompClient && state.stompClient.connected) {
        state.stompClient.send(`/to/call/play/${route.params.roomId}`, JSON.stringify(stage), {})
      }
    }


    const sendGuessName = (message) => {
      state.stompClient.send(`/to/call/guess-name/${route.params.roomId}`, JSON.stringify(message), {})
    }


    const sendDefaultNickname = () => {
      if (state.stompClient && state.stompClient.connected) {
        state.stompClient.send(`/to/call/default-name/${route.params.roomId}/${state.userId}`)
      }
    }


    const joinCallMyRoom = () => {
      console.log('???????????? ???')
      setTimeout(() => {
        state.stompClient.send(`/to/call/addPlayer/${route.params.roomId}`, JSON.stringify(state.userId), {})
      }, 1000)
    }


    const requestMyInfo = () => {
      store.dispatch('root/requestReadMyInfo')
        .then(res => {
        const userInfo = {
            email: res.data.user.email,
            username: res.data.user.username,
            profileImageURL: res.data.user.thumbnailUrl,
            userId: res.data.user.userId,
          }
          store.commit('root/setUserInfo', userInfo)
        })
        .catch(err => {
          ElMessage.err('????????? ??????????????????. ????????? ?????? ??????????????????.')
        })
    }


    const requestUserList = () => {
      store.dispatch('root/requestReadUserList', route.params.roomId)
        .then(res => {
          state.userList = res.data.userList
          for (let i = 0; i < state.userList.length; i++) {
            state.userIdToUserName[state.userList[i].userId] = state.userList[i].username
          }
        })
        .catch(err => {
          ElMessage.err('????????? ??????????????????. ????????? ?????? ??????????????????.')
        })
    }


    const requestRoomInfo = () => {
      store.dispatch('root/requestReadSingleGameRoom', route.params.roomId)
        .then(res => {
          store.commit('root/setRoomInfo', res.data.room)
        })
        .catch(err => {
          ElMessage.err('????????? ??????????????????. ????????? ?????? ??????????????????.')
        })
    }


    const requestUpdateRoomInfo = () => {
      const roomInfo = {
        ...state.room,
        game: null,
        isActivate: true,
      }

      store.dispatch('root/requestUpdateGameRoom', roomInfo)
        .then(res => {
          console.log('???????????? ??????????????? ?????????????????????. ?????? ??????')

        })
        .catch(err => {
          console.log(err)
        })
    }


    const init = () => {
      store.state.root.callmyManager.status = 0;
      store.state.root.callmyManager.isFinished = false;
      store.state.root.callmyManager.nowPlayUsers = [];
      store.state.root.callmyManager.draw =  [];
    }


    const gameOver = () => {
      // ?????? ?????????
      init()
      state.gameStart = false

      if (state.stompClient !== null) {
          console.log('?????? ???????????????')
          state.stompClient.disconnect()
      }

      if (state.room.ownerId === state.userId) requestUpdateRoomInfo()
      endAnswerTime()
      setTimeout(() => {
        // ?????? ?????? ??????
        router.push({
          name: 'gameRoom',
          params: {
            roomId: route.params.roomId,
          }
        })

      }, 5000);
    }


    const showResult = () => {
      setTimeout(() => {
        state.showResult = true
      }, 1000)
      setTimeout(() => {
        state.showResult = false
      }, 4000)
    }


    const endAnswerTime = () => {
      state.speaker = '';
      state.answer = '';
      store.state.root.callmyManager.isAnswer = false;
    }




    requestRoomInfo()
    requestMyInfo()
    requestUserList()
    connectSocket()

    return { state, store, route, sendChat, joinCallMyRoom, sendVote, sendPlay, sendGuessName }
  }
}
</script>

