import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/UseAuth.jsx';
import { apiFetch } from '../../api/api.js';

export const TITLE_MIN = 3;
export const TITLE_MAX = 40;
export const DESC_MIN = 10;
export const DESC_MAX = 700;

function isValid(title, description) {
    const t = title.trim();
    const d = description.trim();
    return t.length >= TITLE_MIN && t.length <= TITLE_MAX
        && d.length >= DESC_MIN && d.length <= DESC_MAX;
}

export function useFeedback() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [isOpen, setIsOpen] = useState(false);
    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [successOpen, setSuccessOpen] = useState(false);
    const [errorOpen, setErrorOpen] = useState(false);

    const mode = user ? 'form' : 'login-required';

    function handleOpen() {
        setIsOpen(true);
    }

    function handleClose() {
        setIsOpen(false);
        setTitle('');
        setDescription('');
    }

    async function handleSubmit() {
        if (!isValid(title, description)) return;
        setIsSubmitting(true);
        const result = await apiFetch('/api/feedback', {
            method: 'POST',
            body: JSON.stringify({ title, description }),
        });
        setIsSubmitting(false);
        if (result.ok) {
            handleClose();
            setSuccessOpen(true);
        } else {
            setErrorOpen(true);
        }
    }

    function navigateToLogin() {
        handleClose();
        navigate('/Login');
    }

    return {
        isOpen,
        mode,
        title,
        description,
        setTitle,
        setDescription,
        isSubmitting,
        isValid: isValid(title, description),
        handleOpen,
        handleClose,
        handleSubmit,
        successOpen,
        dismissSuccess: () => setSuccessOpen(false),
        errorOpen,
        dismissError: () => setErrorOpen(false),
        navigateToLogin,
    };
}
