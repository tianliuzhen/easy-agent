export interface KeyValue {
    key: string;
    value: string;
    description?: string;
}

export interface ParsedCurl {
    method: string;
    url: string;
    headers: KeyValue[];
    params: KeyValue[];
    bodyType: 'none' | 'raw' | 'x-www-form-urlencoded' | 'form-data' | 'binary';
    rawDataType: 'text' | 'json' | 'xml' | 'html';
    rawData: string;
    bodyData: string;
    authType: 'none' | 'bearer' | 'basic' | 'apikey';
    bearerToken: string;
    username: string;
    password: string;
}

// 将 curl 命令拆分为 token，尊重单/双引号与续行符
function tokenize(input: string): string[] {
    const s = input.replace(/\\\r?\n/g, ' ');
    const tokens: string[] = [];
    let i = 0;
    const n = s.length;
    while (i < n) {
        while (i < n && /\s/.test(s[i])) i++;
        if (i >= n) break;
        let token = '';
        while (i < n && !/\s/.test(s[i])) {
            const ch = s[i];
            if (ch === '"' || ch === "'") {
                const quote = ch;
                i++;
                while (i < n && s[i] !== quote) {
                    if (s[i] === '\\' && quote === '"' && i + 1 < n) {
                        token += s[i + 1];
                        i += 2;
                    } else {
                        token += s[i];
                        i++;
                    }
                }
                i++; // 跳过结束引号
            } else if (ch === '\\') {
                if (i + 1 < n) {
                    token += s[i + 1];
                    i += 2;
                } else {
                    i++;
                }
            } else {
                token += ch;
                i++;
            }
        }
        tokens.push(token);
    }
    return tokens;
}

const DATA_FLAGS = ['-d', '--data', '--data-raw', '--data-binary', '--data-ascii', '--data-urlencode'];
// 不带值、可直接忽略的开关
const BOOLEAN_FLAGS = new Set([
    '--compressed', '-i', '--include', '-s', '--silent', '-L', '--location',
    '-k', '--insecure', '-v', '--verbose', '-g', '--globoff', '-#', '--progress-bar',
    '-f', '--fail', '-S', '--show-error', '-O', '--remote-name', '-j', '--junk-session-cookies',
]);

export function parseCurl(input: string): ParsedCurl {
    const tokens = tokenize((input || '').trim());

    let method = '';
    let url = '';
    const headers: KeyValue[] = [];
    let body = '';
    let user = '';

    for (let i = 0; i < tokens.length; i++) {
        const t = tokens[i];
        if (!t) continue;
        if (t === 'curl') continue;

        if (t === '-X' || t === '--request') {
            method = tokens[++i] || '';
            continue;
        }
        if (t === '-H' || t === '--header') {
            const h = tokens[++i] || '';
            const idx = h.indexOf(':');
            if (idx > -1) {
                headers.push({ key: h.slice(0, idx).trim(), value: h.slice(idx + 1).trim(), description: '' });
            }
            continue;
        }
        if (DATA_FLAGS.includes(t)) {
            const d = tokens[++i] || '';
            body += (body ? '&' : '') + d;
            continue;
        }
        if (t === '-u' || t === '--user') {
            user = tokens[++i] || '';
            continue;
        }
        if (t === '--url') {
            url = tokens[++i] || '';
            continue;
        }
        if (t === '-A' || t === '--user-agent') {
            headers.push({ key: 'User-Agent', value: tokens[++i] || '', description: '' });
            continue;
        }
        if (t === '-e' || t === '--referer') {
            headers.push({ key: 'Referer', value: tokens[++i] || '', description: '' });
            continue;
        }
        if (t === '-b' || t === '--cookie') {
            headers.push({ key: 'Cookie', value: tokens[++i] || '', description: '' });
            continue;
        }
        if (BOOLEAN_FLAGS.has(t)) continue;
        if (t.startsWith('-')) {
            // 其它未识别的带值开关：跳过其值（若下一个 token 不是开关）
            const next = tokens[i + 1];
            if (next && !next.startsWith('-')) i++;
            continue;
        }
        // 位置参数视为 URL
        if (!url) url = t;
    }

    if (!method) method = body ? 'POST' : 'GET';
    method = method.toUpperCase();

    // 拆分 URL 上的 query 参数
    const params: KeyValue[] = [];
    let cleanUrl = url;
    const qIdx = url.indexOf('?');
    if (qIdx > -1) {
        cleanUrl = url.slice(0, qIdx);
        const qs = url.slice(qIdx + 1);
        for (const pair of qs.split('&')) {
            if (!pair) continue;
            const eq = pair.indexOf('=');
            const rawKey = eq > -1 ? pair.slice(0, eq) : pair;
            const rawVal = eq > -1 ? pair.slice(eq + 1) : '';
            let key = rawKey;
            let value = rawVal;
            try { key = decodeURIComponent(rawKey); } catch { /* ignore */ }
            try { value = decodeURIComponent(rawVal); } catch { /* ignore */ }
            params.push({ key, value, description: '' });
        }
    }

    // body 类型判断
    let bodyType: ParsedCurl['bodyType'] = 'none';
    let rawDataType: ParsedCurl['rawDataType'] = 'json';
    let rawData = '';
    let bodyData = '';
    if (body) {
        const trimmed = body.trim();
        const contentType = (headers.find(h => h.key.toLowerCase() === 'content-type')?.value || '').toLowerCase();
        if (contentType.includes('application/x-www-form-urlencoded')) {
            bodyType = 'x-www-form-urlencoded';
            bodyData = body.split('&').join('\n');
        } else if (trimmed.startsWith('{') || trimmed.startsWith('[') || contentType.includes('application/json')) {
            bodyType = 'raw';
            rawDataType = 'json';
            rawData = body;
        } else if (contentType.includes('xml')) {
            bodyType = 'raw';
            rawDataType = 'xml';
            rawData = body;
        } else {
            bodyType = 'raw';
            rawDataType = 'text';
            rawData = body;
        }
    }

    // 认证：-u 优先 basic；Authorization: Bearer 转 bearer 并从 headers 移除避免重复
    let authType: ParsedCurl['authType'] = 'none';
    let username = '';
    let password = '';
    let bearerToken = '';
    if (user) {
        authType = 'basic';
        const ci = user.indexOf(':');
        username = ci > -1 ? user.slice(0, ci) : user;
        password = ci > -1 ? user.slice(ci + 1) : '';
    } else {
        const authIdx = headers.findIndex(h => h.key.toLowerCase() === 'authorization');
        if (authIdx > -1 && /^bearer\s+/i.test(headers[authIdx].value)) {
            authType = 'bearer';
            bearerToken = headers[authIdx].value.replace(/^bearer\s+/i, '').trim();
            headers.splice(authIdx, 1);
        }
    }

    return {
        method,
        url: cleanUrl,
        headers,
        params,
        bodyType,
        rawDataType,
        rawData,
        bodyData,
        authType,
        bearerToken,
        username,
        password,
    };
}
