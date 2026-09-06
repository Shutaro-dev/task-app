import type { ChangeEvent } from 'react';
import styles from './RightSidebar.module.css';

interface RightSidebarProps {
  weeklyNotes: string;
  onUpdateWeeklyNotes: (notes: string) => void;
}

function RightSidebar({ weeklyNotes, onUpdateWeeklyNotes }: RightSidebarProps) {
  const updateNotes = (event: ChangeEvent<HTMLTextAreaElement>) => {
    onUpdateWeeklyNotes(event.target.value);
  };

  return (
    <div className={styles['right-sidebar']}>
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
