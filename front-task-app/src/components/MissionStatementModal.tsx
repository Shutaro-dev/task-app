import { useState } from 'react';
import type { ChangeEvent, MouseEvent } from 'react';
import styles from './MissionStatementModal.module.css';

interface MissionStatementModalProps {
  missionStatement: string;
  onClose: () => void;
  onSave: (text: string) => void;
}

function MissionStatementModal({ missionStatement, onClose, onSave }: MissionStatementModalProps) {
  const [text, setText] = useState(missionStatement);

  const handleOverlayClick = (event: MouseEvent<HTMLDivElement>) => {
    if (event.target === event.currentTarget) {
      onClose();
    }
  };

  const handleSave = () => {
    onSave(text.trim());
    onClose();
  };

  return (
    <div className={styles['mission-overlay']} onClick={handleOverlayClick}>
      <div className={styles['mission-modal']} onClick={(e) => e.stopPropagation()}>
        <div className={styles['modal-header']}>
          <h2>Mission Statement</h2>
          <button onClick={onClose} className={styles['close-btn']}>×</button>
        </div>

        <div className={styles['modal-content']}>
          <p className={styles.description}>
            人生で大切にしたい価値観や、なりたい自分の姿を言葉にしましょう。ここに設定した内容は左サイドバー最上部に常に表示され、ロールやタスクを選ぶときの拠り所になります。
          </p>
          <textarea
            value={text}
            onChange={(e: ChangeEvent<HTMLTextAreaElement>) => setText(e.target.value)}
            placeholder="例：家族との時間を大切にしながら、仕事を通じて周囲に貢献し、常に学び続ける人生を送る。"
            className={styles['mission-input']}
            autoFocus
          ></textarea>
        </div>

        <div className={styles['modal-footer']}>
          <button onClick={handleSave} className={styles['save-btn']}>Save</button>
          <button onClick={onClose} className={styles['cancel-btn']}>Cancel</button>
        </div>
      </div>
    </div>
  );
}

export default MissionStatementModal;
