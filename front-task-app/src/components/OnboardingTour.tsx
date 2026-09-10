import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import type { TourStep } from '../onboarding/tourSteps';
import styles from './OnboardingTour.module.css';

const SPOTLIGHT_PADDING = 8;
const GAP = 14;
const VIEWPORT_MARGIN = 12;

interface Position {
  top: number;
  left: number;
}

interface OnboardingTourProps {
  steps: TourStep[];
  onFinish: () => void;
  onSkip: () => void;
}

function OnboardingTour({ steps, onFinish, onSkip }: OnboardingTourProps) {
  const [stepIndex, setStepIndex] = useState(0);
  const [targetRect, setTargetRect] = useState<DOMRect | null>(null);
  const [cardPosition, setCardPosition] = useState<Position | null>(null);

  const cardRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const bodyId = useId();

  const step = steps[stepIndex];
  const isFirst = stepIndex === 0;
  const isLast = stepIndex === steps.length - 1;

  // ターゲット要素の位置を測定する。要素サイズが変わるアニメーション(展開など)を
  // 挟まないため、レイアウト確定後に一度だけ測ればよい(resize/scrollでは追従する)。
  useLayoutEffect(() => {
    const measure = () => {
      if (!step.target) {
        setTargetRect(null);
        return;
      }
      const el = document.querySelector(`[data-tour="${step.target}"]`);
      setTargetRect(el ? el.getBoundingClientRect() : null);
    };
    measure();
    window.addEventListener('resize', measure);
    window.addEventListener('scroll', measure, true);
    return () => {
      window.removeEventListener('resize', measure);
      window.removeEventListener('scroll', measure, true);
    };
  }, [step.target]);

  // 吹き出しカードの実寸が分かってから、ターゲット矩形との位置関係を計算する。
  useLayoutEffect(() => {
    const cardEl = cardRef.current;
    if (!cardEl) return;
    const { width, height } = cardEl.getBoundingClientRect();

    if (!targetRect) {
      setCardPosition({
        top: window.innerHeight / 2 - height / 2,
        left: window.innerWidth / 2 - width / 2,
      });
      return;
    }

    let top: number;
    let left: number;
    switch (step.placement) {
      case 'right':
        left = targetRect.right + GAP;
        top = targetRect.top + 16;
        break;
      case 'left':
        left = targetRect.left - GAP - width;
        top = targetRect.top + 16;
        break;
      case 'top': {
        const edge = step.anchor === 'end' ? targetRect.bottom : targetRect.top;
        top = edge - GAP - height;
        left = targetRect.left + targetRect.width / 2 - width / 2;
        break;
      }
      case 'bottom':
      default: {
        const edge = step.anchor === 'start' ? targetRect.top : targetRect.bottom;
        top = edge + GAP;
        left = targetRect.left + targetRect.width / 2 - width / 2;
        break;
      }
    }

    top = Math.min(Math.max(top, VIEWPORT_MARGIN), window.innerHeight - height - VIEWPORT_MARGIN);
    left = Math.min(Math.max(left, VIEWPORT_MARGIN), window.innerWidth - width - VIEWPORT_MARGIN);
    setCardPosition({ top, left });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetRect, step.placement, step.anchor, stepIndex]);

  // ステップが変わるたびカードにフォーカスを移し、スクリーンリーダーに内容を通知する
  useEffect(() => {
    cardRef.current?.focus();
  }, [stepIndex]);

  const goNext = () => (isLast ? onFinish() : setStepIndex(i => i + 1));
  const goBack = () => setStepIndex(i => Math.max(0, i - 1));

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onSkip();
      else if (e.key === 'ArrowRight' || e.key === 'Enter') goNext();
      else if (e.key === 'ArrowLeft') goBack();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stepIndex]);

  const progressPct = ((stepIndex + 1) / steps.length) * 100;

  return (
    <div className={styles.overlay}>
      {targetRect ? (
        <div
          className={styles.spotlight}
          style={{
            top: targetRect.top - SPOTLIGHT_PADDING,
            left: targetRect.left - SPOTLIGHT_PADDING,
            width: targetRect.width + SPOTLIGHT_PADDING * 2,
            height: targetRect.height + SPOTLIGHT_PADDING * 2,
          }}
        />
      ) : (
        <div className={styles.scrim} />
      )}

      <div
        ref={cardRef}
        className={styles.card}
        style={cardPosition ? { top: cardPosition.top, left: cardPosition.left } : { opacity: 0 }}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={bodyId}
        tabIndex={-1}
        key={step.id}
      >
        <div className={styles.progressRow}>
          <div className={styles.progressTrack}>
            <div className={styles.progressFill} style={{ width: `${progressPct}%` }} />
          </div>
          <button className={styles.skipBtn} onClick={onSkip} aria-label="ツアーをスキップ" type="button">
            <i className="bi bi-x-lg"></i>
          </button>
        </div>
        <span className={styles.stepCount}>
          {String(stepIndex + 1).padStart(2, '0')} / {String(steps.length).padStart(2, '0')}
        </span>

        <h3 id={titleId} className={styles.title}>{step.title}</h3>
        <p id={bodyId} className={styles.body}>{step.body}</p>

        <div className={styles.footer}>
          {!isFirst ? (
            <button className={styles.backBtn} onClick={goBack} type="button">戻る</button>
          ) : <span />}
          <button className={styles.nextBtn} onClick={goNext} type="button">
            {isFirst ? 'はじめる' : isLast ? 'はじめましょう' : '次へ'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default OnboardingTour;
