import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Practice from '../views/Practice.vue'
import MyRecords from '../views/MyRecords.vue'
import PartPractice from '../views/PartPractice.vue'
import FullTest from '../views/FullTest.vue'
import KeyExpressions from '../views/KeyExpressions.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/practice', component: Practice },
  { path: '/practice/:part', component: PartPractice, props: true },
  { path: '/full-test', component: FullTest },
  { path: '/my-records', component: MyRecords },
  { path: '/key-expressions', component: KeyExpressions }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
