package self.tranluunghia.selectimage.extensions

import androidx.fragment.app.FragmentManager

fun FragmentManager.removeAllFragments(): FragmentManager {
    for (fragment in this.fragments) {
        this.beginTransaction().remove(fragment).commit()
    }
    return this
}