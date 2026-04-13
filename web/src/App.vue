<template>
  <div class="app appLayout">
    <aside class="appNav">
      <div class="appNavBrand">
        <div class="logo">CB</div>
        <div>
          <div class="title">ChatBase</div>
          <div class="sub">控制台</div>
        </div>
      </div>
      <nav class="appNavList">
        <RouterLink class="navItem" to="/console/dashboard" active-class="active">概览</RouterLink>
        <RouterLink class="navItem" to="/console/im" active-class="active">QQ / 企微群聊</RouterLink>
        <RouterLink class="navItem" to="/chat" active-class="active">Web 问答</RouterLink>
        <RouterLink class="navItem" to="/upload" active-class="active">知识库上传</RouterLink>
      </nav>
      <div class="appNavFoot muted">采集与 Dify 问答</div>
    </aside>
    <div class="appBody">
      <main class="appMain">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
