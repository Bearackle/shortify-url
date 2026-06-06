"use client";

import Image from "next/image";
import { useState, KeyboardEvent } from "react";

export default function Home() {
    const [inputUrl, setInputUrl] = useState("");
    const [shortCode, setShortCode] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [copied, setCopied] = useState(false);

    const handleKeyDown = async (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key !== "Enter") return;
        const url = inputUrl.trim();
        if (!url) return;

        setShortCode("");
        setError("");
        setLoading(true);

        try {
            const res = await fetch("http://localhost:8080/url", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ url }),
            });

            if (!res.ok) throw new Error("Server error");

            const data = await res.json();
            setShortCode(data.shortCode);
        } catch {
            setError("Không thể kết nối đến server.");
        } finally {
            setLoading(false);
        }
    };

    const handleCopy = () => {
        navigator.clipboard.writeText(`http://localhost:8080/${shortCode}`);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <div className="flex flex-col flex-1 items-center justify-center bg-zinc-50 font-sans dark:bg-black">
            <main className="flex flex-1 w-full max-w-3xl flex-col items-center justify-between py-32 px-16 bg-white dark:bg-black sm:items-start">
                <Image
                    className="dark:invert"
                    src="/next.svg"
                    alt="Next.js logo"
                    width={100}
                    height={20}
                    priority
                />

                <div className="w-full mt-12 flex flex-col gap-3">
                    {/* Input URL dài */}
                    <label className="text-xs font-medium text-zinc-400 uppercase tracking-widest">
                        URL gốc
                    </label>
                    <input
                        type="text"
                        value={inputUrl}
                        onChange={(e) => setInputUrl(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="https://example.com/very/long/url..."
                        className="w-full px-4 py-3 rounded-xl border border-zinc-200 bg-zinc-50 font-mono text-sm text-zinc-800 outline-none focus:border-zinc-800 focus:ring-2 focus:ring-zinc-100 transition dark:bg-zinc-900 dark:border-zinc-700 dark:text-zinc-100 dark:focus:border-zinc-400"
                    />
                    {/* Loading */}
                    {loading && (
                        <p className="text-sm text-zinc-400 animate-pulse">Đang rút gọn...</p>
                    )}

                    {/* Lỗi */}
                    {error && (
                        <p className="text-sm text-red-500 font-mono">{error}</p>
                    )}

                    {/* Kết quả */}
                    {shortCode && (
                        <div className="flex flex-col gap-2 mt-1">
                            <label className="text-xs font-medium text-zinc-400 uppercase tracking-widest">
                                Short URL
                            </label>
                            <div className="flex items-center gap-2 bg-zinc-50 border border-zinc-200 rounded-xl px-4 py-3 dark:bg-zinc-900 dark:border-zinc-700">
                <span className="flex-1 font-mono text-sm text-zinc-800 dark:text-zinc-100 truncate">
                  http://localhost:8080/{shortCode}
                </span>
                                <button
                                    onClick={handleCopy}
                                    className={`shrink-0 px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                                        copied
                                            ? "bg-green-600 text-white"
                                            : "bg-zinc-800 text-white hover:bg-zinc-600 dark:bg-zinc-200 dark:text-zinc-900"
                                    }`}
                                >
                                    {copied ? "Copied!" : "Copy"}
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}