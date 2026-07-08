import { useEffect, useState } from 'react';
import { apiFetch } from '../../../api/api.js';

export default function useAccountDeletion() {
    const [deleteMode, setDeleteMode] = useState('idle'); // 'idle' | 'otp'
    const [deleteCode, setDeleteCode] = useState('');
    const [deleteAllDataNow, setDeleteAllDataNow] = useState(false);
    const [deleteError, setDeleteError] = useState('');
    const [deleteResendDisabled, setDeleteResendDisabled] = useState(false);
    const [deleteCountdown, setDeleteCountdown] = useState(0);
    const [saving, setSaving] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [showDeletedInfo, setShowDeletedInfo] = useState(false);

    useEffect(() => {
        if (deleteCountdown <= 0) {
            setDeleteResendDisabled(false);
            return;
        }
        const timer = setTimeout(() => setDeleteCountdown(deleteCountdown - 1), 1000);
        return () => clearTimeout(timer);
    }, [deleteCountdown]);

    async function requestDeleteCode(rateLimitedMessage) {
        setSaving(true);
        setDeleteError('');
        const res = await apiFetch('/api/players/me/delete-request', { method: 'POST' });
        setSaving(false);

        if (res.ok) {
            setDeleteMode('otp');
            setDeleteResendDisabled(true);
            setDeleteCountdown(60);
            return;
        }

        if (res.status === 429) {
            const waitTime = res.data?.['seconds-to-wait'] || 60;
            setDeleteError(rateLimitedMessage(waitTime));
            setDeleteMode('otp');
            setDeleteResendDisabled(true);
            setDeleteCountdown(waitTime);
        } else {
            setDeleteError('Nie udało się rozpocząć procedury usuwania. Spróbuj ponownie.');
        }
    }

    function handleInitiateDelete() {
        setShowDeleteConfirm(true);
    }

    function cancelDeleteConfirm() {
        setShowDeleteConfirm(false);
    }

    async function confirmDeleteRequest() {
        setShowDeleteConfirm(false);
        await requestDeleteCode(() => 'Zbyt wiele prób. Kod z poprzedniego żądania jest wciąż ważny.');
    }

    async function handleDeleteResend() {
        await requestDeleteCode((waitTime) => `Zbyt wiele prób. Możesz wysłać kolejny kod za ${waitTime} sekund.`);
    }

    async function handleConfirmDelete(e) {
        e.preventDefault();
        setSaving(true);
        setDeleteError('');

        const res = await apiFetch('/api/players/me/delete-confirm', {
            method: 'POST',
            body: JSON.stringify({ code: deleteCode, deleteAllDataNow }),
        });
        setSaving(false);

        if (res.ok) {
            setShowDeletedInfo(true);
            return;
        }

        if (res.status === 403 || res.status === 404 || res.status === 410) {
            setDeleteError('Nieprawidłowy lub wygasły kod weryfikacyjny.');
        } else {
            setDeleteError('Błąd serwera. Nie udało się usunąć konta.');
        }
    }

    function cancelOtp() {
        setDeleteMode('idle');
        setDeleteError('');
    }

    function dismissDeletedInfo() {
        setShowDeletedInfo(false);
    }

    return {
        deleteMode,
        deleteCode,
        setDeleteCode,
        deleteAllDataNow,
        setDeleteAllDataNow,
        deleteError,
        deleteResendDisabled,
        deleteCountdown,
        saving,
        showDeleteConfirm,
        showDeletedInfo,
        handleInitiateDelete,
        cancelDeleteConfirm,
        confirmDeleteRequest,
        handleDeleteResend,
        handleConfirmDelete,
        cancelOtp,
        dismissDeletedInfo,
    };
}
