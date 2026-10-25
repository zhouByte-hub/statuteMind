const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }

export const escapeHtml = s => s.replace(/[&<>"']/g, c => ESC[c])

/** 关键词命中高亮：先转义再包 <b class="hl">，返回受控 HTML（仅配合 v-html 使用） */
export function highlight(text, kws){
  const uniq = [...new Set(kws.filter(Boolean))].sort((a, b) => b.length - a.length)
  const safe = escapeHtml(text)
  if(!uniq.length) return safe
  const re = new RegExp('(' + uniq.map(k => k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|') + ')', 'g')
  return safe.replace(re, '<b class="hl">$1</b>')
}
