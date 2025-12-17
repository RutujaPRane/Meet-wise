import React from 'react';
import {createRoot} from 'react-dom/client';
import { FileUpload } from './Components/FileUpload.jsx';
import './index.css';
import { App } from './Components/App.jsx';

const root = createRoot(document.getElementById('root'));

root.render(<div className='upload'><App /></div>);