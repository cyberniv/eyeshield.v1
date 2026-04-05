document.addEventListener('DOMContentLoaded', () => {
    const widget = document.querySelector('.confidence-widget');
    const progressRing = document.querySelector('.confidence-progress');
    const valueNode = document.querySelector('.confidence-value');

    if (!widget || !progressRing || !valueNode) {
        return;
    }

    const targetScore = Number(widget.dataset.score || 65);
    const arcLength = 74;
    const duration = 1200;
    const start = performance.now();

    const render = (score) => {
        const clamped = Math.max(0, Math.min(100, score));
        const filledLength = arcLength * (clamped / 100);
        const offset = arcLength - filledLength;

        progressRing.style.setProperty('--progress-offset', offset.toFixed(2));
        valueNode.textContent = Math.round(clamped).toString();
    };

    const animate = (timestamp) => {
        const elapsed = timestamp - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);

        render(targetScore * eased);

        if (progress < 1) {
            requestAnimationFrame(animate);
        }
    };

    render(0);
    requestAnimationFrame(animate);
});