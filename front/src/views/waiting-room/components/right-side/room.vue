<template>
  <div :class="{'game-room-item': true, 'game-room-disable': !room.isActivate}">
    <div class="header">
      <div class="game-room-title">{{ room.title }}</div>
      <div class="game-room-count-secret">
        <i v-if="room.password !== 0" class="el-icon-lock"></i>
        <div class="game-room-count">{{ user ? user.length : 0 }} / {{ room.maxPlayer }}</div>
      </div>
    </div>
    <div class="content">
      <div class="thumbnail">
        <img v-if="room.game === null" :src="require('@/assets/images/game-thumbnail-default.png')" alt="">
        <img v-if="room.game === 'mafia'" :src="require('@/assets/images/game-thumbnail-mafia.png')" alt="">
        <img v-if="room.game === 'callmy'" :src="require('@/assets/images/game-thumbnail-callmy.png')" alt="">
      </div>
      <div class="status">
        <div class="selected-game">Now selected {{ room.game ? room.game : 'None' }}</div>
        <div class="play-or-wait">
          <div class="wait" v-if="room.isActivate">Waiting</div>
          <div class="play" v-else>Playing</div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  name: 'Home',

  props: {
    room: {
      type: Object,
    },
    user: {
      type: Object,
    }
  },

  setup () {}
}
</script>
<style scoped>
  @import url('./room.css');
</style>

