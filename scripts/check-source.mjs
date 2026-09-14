// Dependency-free structural checks. These do NOT replace Java compilation or Thymeleaf rendering.
import fs from 'node:fs';
import path from 'node:path';
const walk = dir => fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]);
const sources=walk('src'); let checked=0;const errors=[];const routes=new Map();
for(const file of sources){
 if(!/\.(java|html|css|js|sql|yml)$/.test(file))continue;
 const text=fs.readFileSync(file,'utf8');checked++;
 if(file.endsWith('.java')){
  const code=text.replace(/"""[\s\S]*?"""/g,'').replace(/"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\/\*[\s\S]*?\*\/|\/\/[^\n]*/g,'');
  const stack=[];const close={')':'(',']':'[','}':'{'};
  for(const char of code){if('([{'.includes(char))stack.push(char);else if(')]}'.includes(char)&&stack.pop()!==close[char])errors.push(file+': unbalanced delimiter');}
  if(stack.length)errors.push(file+': unclosed delimiter');
  if(!file.includes('/test/'))for(const match of text.matchAll(/@(Get|Post)Mapping\(([^)]*)\)/g))for(const value of match[2].matchAll(/"([^"]+)"/g)){
   const key=match[1]+' '+value[1];if(routes.has(key))errors.push('Duplicate route '+key+' in '+file);routes.set(key,file);
  }
 }
 if(file.endsWith('.html')){
  for(const match of text.matchAll(/~\{([\w/.-]+)\s*::/g))if(!fs.existsSync('src/main/resources/templates/'+match[1]+'.html'))errors.push(file+': missing fragment '+match[1]);
  if(text.includes('hasAnyRole(\'STAFF\',\'ADMIN\')')&&/\/(stay|request|reservation)\//.test(file))errors.push(file+': obsolete module permission');
 }
}
console.log(`${checked} source/resource files checked; ${routes.size} unique GET/POST mappings.`);
if(errors.length){console.error([...new Set(errors)].join('\n'));process.exit(1);}
console.log('PASS: structural checks. Java compilation, SQL execution and rendered browser checks remain separate gates.');
