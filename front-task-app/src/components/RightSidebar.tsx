import type { ChangeEvent } from 'react';
import styles from './RightSidebar.module.css';

interface RightSidebarProps {
  weeklyNotes: string;
  missionStatement: string;
  onUpdateWeeklyNotes: (notes: string) => void;
  onOpenMissionSettings: () => void;
}

function RightSidebar({ weeklyNotes, missionStatement, onUpdateWeeklyNotes, onOpenMissionSettings }: RightSidebarProps) {
  const updateNotes = (event: ChangeEvent<HTMLTextAreaElement>) => {
    onUpdateWeeklyNotes(event.target.value);
  };

  return (
    <div className={styles['right-sidebar']}>
      <div className={styles['mission-summary']}>
        <div className={styles['mission-header']}>
          <h3>Mission Statement</h3>
          <i className={`bi bi-pencil ${styles['edit-mission-btn']}`} onClick={onOpenMissionSettings}></i>
        </div>
        {missionStatement ? (
          <p className={styles['mission-text']} onClick={onOpenMissionSettings}>{missionStatement}</p>
        ) : (
          <button onClick={onOpenMissionSettings} className={styles['mission-empty-btn']}>
            + ミッションステートメントを設定
          </button>
        )}
      </div>

      <div className={styles['notes-section']}>
        <h3>Weekly Notes</h3>
        <textarea
          value={weeklyNotes}
          onChange={updateNotes}
          placeholder="Weekly reflections, goals, insights..."
          className={styles['weekly-notes-input']}
        ></textarea>
      </div>

      <div className={styles['info-section']}>
        <h4>Quick Tips</h4>
        <ul className={styles['tips-list']}>
          <li>Drag tasks from roles to calendar</li>
          <li>Click day headers to set sleep times</li>
          <li>Use daily notes for reflection</li>
          <li>Balance roles with renewal activities</li>
        </ul>
      </div>
    </div>
  );
}

export default RightSidebar;
