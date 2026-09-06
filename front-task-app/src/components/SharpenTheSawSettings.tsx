import { useState } from 'react';
import type { MouseEvent } from 'react';
import type { SharpenTheSawArea } from '../types';
import styles from './SharpenTheSawSettings.module.css';

interface SharpenTheSawSettingsProps {
  areas: SharpenTheSawArea[];
  onClose: () => void;
  onUpdateAreas: (areas: SharpenTheSawArea[]) => void;
}

function SharpenTheSawSettings({ areas, onClose, onUpdateAreas }: SharpenTheSawSettingsProps) {
  const [localAreas, setLocalAreas] = useState<SharpenTheSawArea[]>(
    () => JSON.parse(JSON.stringify(areas)) as SharpenTheSawArea[]
  );

  const handleOverlayClick = (event: MouseEvent<HTMLDivElement>) => {
    if (event.target === event.currentTarget) {
      onClose();
    }
  };

  const addTask = (areaId: string) => {
    setLocalAreas(prev => prev.map(area => {
      if (area.id !== areaId) return area;
      return {
        ...area,
        tasks: [...area.tasks, { id: Date.now().toString(), title: '', roleId: 'renewal', isPermanent: true }],
      };
    }));
  };

  const removeTask = (areaId: string, taskIndex: number) => {
    setLocalAreas(prev => prev.map(area => {
      if (area.id !== areaId) return area;
      return { ...area, tasks: area.tasks.filter((_, i) => i !== taskIndex) };
    }));
  };

  const updateTaskTitle = (areaId: string, taskIndex: number, title: string) => {
    setLocalAreas(prev => prev.map(area => {
      if (area.id !== areaId) return area;
      const tasks = area.tasks.map((t, i) => (i === taskIndex ? { ...t, title } : t));
      return { ...area, tasks };
    }));
  };

  const handleSave = () => {
    const cleaned = localAreas.map(area => ({
      ...area,
      tasks: area.tasks.filter(task => task.title.trim() !== ''),
    }));
    onUpdateAreas(cleaned);
    onClose();
  };

  return (
    <div className={styles['settings-overlay']} onClick={handleOverlayClick}>
      <div className={styles['settings-modal']} onClick={(e) => e.stopPropagation()}>
        <div className={styles['modal-header']}>
          <h2>Sharpen the Saw Settings</h2>
          <button onClick={onClose} className={styles['close-btn']}>×</button>
        </div>

        <div className={styles['modal-content']}>
          <p className={styles.description}>
            Set your renewal goals and permanent tasks for each area of personal development.
          </p>

          <div className={styles['areas-grid']}>
            {localAreas.map(area => (
              <div key={area.id} className={styles['area-section']}>
                <div className={styles['area-header']}>
                  <span className={styles['area-icon']}>{area.icon}</span>
                  <h3>{area.name}</h3>
                </div>

                <div className={styles['area-content']}>
                  <div className={styles['tasks-section']}>
                    <h4>Renewal Tasks</h4>
                    {area.tasks.map((task, index) => (
                      <div key={task.id} className={styles['task-item']}>
                        <input
                          value={task.title}
                          onChange={(e) => updateTaskTitle(area.id, index, e.target.value)}
                          placeholder="Task title"
                          className={styles['task-input']}
                        />
                        <button
                          onClick={() => removeTask(area.id, index)}
                          className={styles['remove-task-btn']}
                        >
                          ×
                        </button>
                      </div>
                    ))}

                    <button onClick={() => addTask(area.id)} className={styles['add-task-btn']}>
                      + Add Task
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className={styles['modal-footer']}>
          <button onClick={handleSave} className={styles['save-btn']}>Save Settings</button>
          <button onClick={onClose} className={styles['cancel-btn']}>Cancel</button>
        </div>
      </div>
    </div>
  );
}

export default SharpenTheSawSettings;
