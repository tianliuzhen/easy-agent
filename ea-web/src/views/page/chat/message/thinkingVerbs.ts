import {useEffect, useRef, useState} from 'react';

/**
 * 思考中的轮换提示词（英文）。
 * <p>
 * 生成期间等待时间可能很长，固定一句「思考中」会让人以为卡住了；轮换的提示
 * 既表明还在跑，也让等待不那么枯燥。展示时统一追加省略号。
 */
export const THINKING_VERBS: string[] = [
    'Thinking',
    'Pondering',
    'Reasoning',
    'Reflecting',
    'Deliberating',
    'Contemplating',
    'Mulling',
    'Noodling',
    'Ruminating',
    'Cogitating',
    'Musing',
    'Puzzling',
    'Percolating',
    'Brainstorming',
    'Untangling',
    'Unraveling',
    'Connecting dots',
    'Digging deeper',
    'Working it out',
    'Piecing together',
    'Weighing options',
    'Crunching',
    'Scheming',
    'Strategizing',
    'Synthesizing',
    'Analyzing',
    'Investigating',
    'Exploring',
    'Calculating',
    'Formulating',
    'Spelunking',
    'Wrangling',
    'Tinkering',
    'Marinating',
    'Simmering',
    'Vibing',
];

/** 轮换间隔，单位：毫秒 */
const ROTATE_INTERVAL_MS = 5000;

const randomIndex = (exclude?: number): number => {
    if (THINKING_VERBS.length < 2) return 0;
    let next = Math.floor(Math.random() * THINKING_VERBS.length);
    // 避免连续两次抽到同一个，否则看起来像停住了
    while (next === exclude) {
        next = Math.floor(Math.random() * THINKING_VERBS.length);
    }
    return next;
};

/**
 * 生成期间每 5 秒随机换一个思考提示词。
 *
 * @param active 是否处于生成中；为 false 时停掉定时器并返回 null
 * @return 当前要展示的提示词；未激活时为 null
 */
export const useThinkingVerb = (active: boolean): string | null => {
    // 初始值即随机，每条消息的开场各不相同；不在 effect 里再随机一次，
    // 否则挂载瞬间会连换两个词，看起来像闪了一下
    const [index, setIndex] = useState(() => randomIndex());
    const indexRef = useRef(index);
    indexRef.current = index;

    useEffect(() => {
        if (!active) return;
        const timer = window.setInterval(() => {
            setIndex(randomIndex(indexRef.current));
        }, ROTATE_INTERVAL_MS);
        return () => window.clearInterval(timer);
    }, [active]);

    return active ? THINKING_VERBS[index] : null;
};
