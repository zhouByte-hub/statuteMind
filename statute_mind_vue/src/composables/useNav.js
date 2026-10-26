import { useRouter, useRoute } from 'vue-router'
import { store } from '@/store/app'

/** 跨视图跳转：同参重复跳转（路由不变）时改发回放信号，保证目标视图可重放 */
export function useNav(){
  const router = useRouter()
  const route = useRoute()

  const openSearch = q => {
    if(route.name === 'search' && route.query.q === q) store.requestReplay('search', q)
    else router.push({ name: 'search', query: { q } })
  }
  const openJudgment = jid => {
    if(route.name === 'judge' && route.query.j === jid) store.requestReplay('judge', jid)
    else router.push({ name: 'judge', query: { j: jid } })
  }
  const openLibrary = libId => {
    if(route.name === 'library' && route.query.lib === libId) store.requestReplay('library', libId)
    else router.push({ name: 'library', query: { lib: libId } })
  }
  /** 打开工作流编排画布 */
  const openWorkflow = () => router.push({ name: 'workflow' })
  /** 回到检索首页态（清除 q） */
  const goHome = () => router.push({ name: 'search' })

  return { openSearch, openJudgment, openLibrary, openWorkflow, goHome }
}
