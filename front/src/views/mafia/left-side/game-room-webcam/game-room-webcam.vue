<template>
  <div v-if="!gameStart" class="before-game-start">
    <div>플레이어가 입장 중  ( {{ state.joinedPlayerNumbers }} / {{ playerNumber }} )</div>
  </div>
  <div
    :class="{
      'webcam-container': true,
      'under-two': state.subscribers.length >= 1,
      'under-four': state.subscribers.length >= 2,
      'under-nine': state.subscribers.length >= 4,
    }">
    <user-video id="myWebcam"
      :gameStart="gameStart"
      :stream-manager="state.publisher"/>
    <user-video
      v-for="sub in state.subscribers"
      :gameStart="gameStart"
      :key="sub.stream.connection.connectionId"
      :stream-manager="sub"/>
  </div>
</template>
<script>
import $axios from 'axios'
import { computed, reactive, onBeforeUnmount, watch } from 'vue'
import { OpenVidu } from 'openvidu-browser'
import { useStore } from 'vuex'
import UserVideo from './components/UserVideo'

$axios.defaults.headers.post['Content-Type'] = 'application/json'
$axios.defaults.headers.post['Access-Control-Allow-Origin'] = '*'

export default {
  components: {
		UserVideo,
	},

  props: {
    roomId: {
      type: Number
    },
    playerNumber: {
      type: Number
    },
    gameStart: {
      type: Boolean
    }
  },

  setup(props, { emit }) {
    const OPENVIDU_SERVER_URL = "https://" + location.hostname + ":8443"
    const OPENVIDU_SERVER_SECRET = "NARANG_VIDU"
    const store = useStore()

    const state = reactive({
			OV: undefined,
			session: undefined,
			mainStreamManager: undefined,
			publisher: undefined,
			subscribers: [],
			mySessionId: computed(() => props.roomId),
			myUserName: computed(() => store.getters['root/username']),
      joinedPlayerNumbers: 0,
    })

    const joinSession = () => {
			// --- Get an OpenVidu object ---
			state.OV = new OpenVidu()

			// --- Init a session ---
			state.session = state.OV.initSession()

			// On every new Stream received...
			state.session.on('streamCreated', ({ stream }) => {
				const subscriber = state.session.subscribe(stream)
				state.subscribers.push(subscriber)
        if (subscriber.videos !== []) {
          state.joinedPlayerNumbers++
        }
			})

			// On every Stream destroyed...
			state.session.on('streamDestroyed', ({ stream }) => {
				const index = state.subscribers.indexOf(stream.streamManager, 0)
        if (index >= 0) {
          const subscriber = state.subscribers[index]
          if (subscriber.videos !== []) state.joinedPlayerNumbers--
          state.subscribers.splice(index, 1)
        }
			})

			// On every asynchronous exception...
			state.session.on('exception', ({ exception }) => {
				console.warn(exception)
			})

			// 'getToken' method is simulating what your server-side should do.
			// 'token' parameter should be retrieved and returned by your own backend
			getToken(state.mySessionId).then(token => {
				state.session.connect(token, { clientData: state.myUserName })
					.then(() => {

						// --- Get your own camera stream with the desired properties ---
						let publisher = state.OV.initPublisher(undefined, {
							audioSource: undefined, // The source of audio. If undefined default microphone
							videoSource: undefined, // The source of video. If undefined default webcam
							publishAudio: true,  	// Whether you want to start publishing with your audio unmuted or not
							publishVideo: true,  	// Whether you want to start publishing with your video enabled or not
							resolution: '600x320',  // The resolution of your video
							frameRate: 30,			// The frame rate of your video
							insertMode: 'APPEND',	// How the video is inserted in the target element 'video-container'
							mirror: true,       	// Whether to mirror your local video or not
						})

						state.mainStreamManager = publisher
						state.publisher = publisher
            store.state.root.publisher = publisher
            state.joinedPlayerNumbers++
						state.session.publish(state.publisher)
					})
					.catch(error => {
						console.log('There was an error connecting to the session:', error.code, error.message)
					})
			})

			window.addEventListener('beforeunload', leaveSession)
		}

    const leaveSession = () => {
			// --- Leave the session by calling 'disconnect' method over the Session object ---
			if (state.session) state.session.disconnect()

			state.session = undefined
			state.mainStreamManager = undefined
			state.publisher = undefined
			state.subscribers = []
			state.OV = undefined

			window.removeEventListener('beforeunload', leaveSession)
		}

    const getToken = (mySessionId) => {
			return createSession(mySessionId).then(sessionId => createToken(sessionId))
		}

		const createSession = (sessionId) => {
			return new Promise((resolve, reject) => {
				$axios
					.post(`${OPENVIDU_SERVER_URL}/openvidu/api/sessions`, JSON.stringify({
						customSessionId: sessionId,
					}), {
						auth: {
							username: 'OPENVIDUAPP',
							password: OPENVIDU_SERVER_SECRET,
						},
					})
					.then(response => response.data)
					.then(data => resolve(data.id))
					.catch(error => {
						if (error.response.status === 409) {
							resolve(sessionId)
						} else {
							console.warn(`No connection to OpenVidu Server. This may be a certificate error at ${OPENVIDU_SERVER_URL}`)
							if (window.confirm(`No connection to OpenVidu Server. This may be a certificate error at ${OPENVIDU_SERVER_URL}\n\nClick OK to navigate and accept it. If no certificate warning is shown, then check that your OpenVidu Server is up and running at "${OPENVIDU_SERVER_URL}"`)) {
								location.assign(`${OPENVIDU_SERVER_URL}/accept-certificate`)
							}
							reject(error.response)
						}
					})
			})
		}

    const createToken = (sessionId) => {
			return new Promise((resolve, reject) => {
				$axios
					.post(`${OPENVIDU_SERVER_URL}/openvidu/api/sessions/${sessionId}/connection`, {
              "type": "WEBRTC",
              "role": "PUBLISHER",
              "kurentoOptions": {
                "videoMaxRecvBandwidth": 1000,
                "videoMinRecvBandwidth": 300,
                "videoMaxSendBandwidth": 1000,
                "videoMinSendBandwidth": 300,
                "allowedFilters": ["GStreamerFilter", "FaceOverlayFilter"]
              }
          },
          {
						auth: {
							username: 'OPENVIDUAPP',
							password: OPENVIDU_SERVER_SECRET,
						},
					})
					.then(response => response.data)
					.then(data => resolve(data.token))
					.catch(error => reject(error.response))
			})
		}

    // created
    watch(() => props.playerNumber, () => {
      if (props.playerNumber >= 1) joinSession()
    })


    watch(() => state.joinedPlayerNumbers, () => {
      console.log(state.joinedPlayerNumbers, props.playerNumber, '하이')
      if (state.joinedPlayerNumbers === props.playerNumber) emit('sendAddPlayer')
    })

    // beforeunmount
    onBeforeUnmount(() => {
      leaveSession()
    })
    return { state }
  },
}
</script>
<style scoped>
  @import url('./game-room-webcam.css');
</style>
